package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ProductSaleRequest;
import com.appointflow.dto.ProductSaleResponse;
import com.appointflow.dto.ProductSalesSummaryResponse;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSaleService {

    private final ProductSaleRepository productSaleRepository;
    private final ProductRepository productRepository;
    private final RandevuRepository randevuRepository;
    private final KullaniciRepository kullaniciRepository;
    private final EarningRepository earningRepository;
    private final CommissionService commissionService;

    /**
     * Randevuya urun satisi ekler:
     * 1) Stok kontrolu ve dusum
     * 2) ProductSale kaydi (fiyat snapshot)
     * 3) PRODUCT scope komisyon kurali varsa Earning kaydi (PENDING)
     */
    @Transactional
    public ProductSaleResponse addSaleToAppointment(Long randevuId, ProductSaleRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Randevu randevu = randevuRepository.findById(randevuId)
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));
        if (!randevu.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("Urun bulunamadi: " + request.getProductId()));
        if (!product.getTenantId().equals(tenantId)) {
            throw ApiException.notFound("Urun bulunamadi: " + request.getProductId());
        }
        if (!Boolean.TRUE.equals(product.getAktif())) {
            throw ApiException.badRequest("Urun aktif degil: " + product.getAd());
        }
        if (product.getStok() == null || product.getStok() < request.getAdet()) {
            throw ApiException.badRequest(
                    String.format("Yetersiz stok: %s (mevcut: %d, istenen: %d)",
                            product.getAd(),
                            product.getStok() != null ? product.getStok() : 0,
                            request.getAdet()));
        }

        BigDecimal birimFiyat = product.getFiyat().setScale(2, RoundingMode.HALF_UP);
        BigDecimal toplam = birimFiyat.multiply(BigDecimal.valueOf(request.getAdet()))
                .setScale(2, RoundingMode.HALF_UP);

        Long staffId = randevu.getUzman() != null ? randevu.getUzman().getId() : null;
        Long customerId = randevu.getCustomer() != null ? randevu.getCustomer().getId() : null;

        // Komisyon hesabi (PRODUCT scope kurali varsa) — 4 seviye lookup:
        // (staff+product) > (staff+kategori) > (tenant+product) > (tenant+kategori) > staff genel > tenant genel
        Optional<CommissionRule> ruleOpt = commissionService.findProductRule(
                tenantId, staffId, product.getId(), product.getKategori());
        BigDecimal rateSnapshot = null;
        BigDecimal commissionAmount = null;
        if (ruleOpt.isPresent() && staffId != null) {
            CommissionRule rule = ruleOpt.get();
            rateSnapshot = rule.getRate();
            commissionAmount = commissionService.calculateCommissionAmount(rule, toplam);
        }

        // ProductSale kaydet
        ProductSale sale = ProductSale.builder()
                .tenantId(tenantId)
                .randevu(randevu)
                .product(product)
                .staffId(staffId)
                .customerId(customerId)
                .adet(request.getAdet())
                .birimFiyatSnapshot(birimFiyat)
                .toplamTutar(toplam)
                .commissionRateSnapshot(rateSnapshot)
                .commissionAmount(commissionAmount)
                .build();
        ProductSale saved = productSaleRepository.save(sale);

        // Stok dusur
        product.setStok(product.getStok() - request.getAdet());
        productRepository.save(product);

        // Randevu toplam fiyatını güncelle (hizmetler + ürünler)
        recomputeAppointmentTotal(randevu);

        // Earning kaydi (komisyon varsa)
        if (ruleOpt.isPresent() && staffId != null && commissionAmount != null) {
            CommissionRule rule = ruleOpt.get();
            BigDecimal net = toplam.subtract(commissionAmount);
            Earning earning = Earning.builder()
                    .tenantId(tenantId)
                    .randevu(randevu)
                    .staffId(staffId)
                    .commissionRuleId(rule.getId())
                    .productSaleId(saved.getId())
                    .commissionType(rule.getCommissionType())
                    .rateSnapshot(rateSnapshot)
                    .grossAmount(toplam)
                    .commissionAmount(commissionAmount)
                    .netAmount(net)
                    .status(EarningStatus.PENDING)
                    .build();
            earningRepository.save(earning);
            log.debug("Urun satisi earning olusturuldu: saleId={}, staffId={}, commission={}",
                    saved.getId(), staffId, commissionAmount);
        }

        return toResponse(saved);
    }

    /**
     * Randevu olmadan satış (dashboard hızlı satış). customerId ve staffId opsiyonel.
     * Komisyon: staffId verilirse PRODUCT scope kural varsa Earning oluşur.
     */
    @Transactional
    public ProductSaleResponse createStandalone(Long productId, int adet, Long customerId, Long staffId) {
        Long tenantId = TenantContext.getTenantId();
        if (adet < 1) throw ApiException.badRequest("Adet en az 1 olmalı.");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Urun bulunamadi: " + productId));
        if (!product.getTenantId().equals(tenantId)) {
            throw ApiException.notFound("Urun bulunamadi: " + productId);
        }
        if (!Boolean.TRUE.equals(product.getAktif())) {
            throw ApiException.badRequest("Urun aktif degil: " + product.getAd());
        }
        if (product.getStok() == null || product.getStok() < adet) {
            throw ApiException.badRequest(
                    String.format("Yetersiz stok: %s (mevcut: %d, istenen: %d)",
                            product.getAd(),
                            product.getStok() != null ? product.getStok() : 0,
                            adet));
        }

        // Staff varsa tenant kontrol
        if (staffId != null) {
            Kullanici staff = kullaniciRepository.findById(staffId)
                    .orElseThrow(() -> ApiException.notFound("Calisan bulunamadi: " + staffId));
            if (!staff.getTenantId().equals(tenantId)) {
                throw ApiException.notFound("Calisan bulunamadi: " + staffId);
            }
        }

        BigDecimal birimFiyat = product.getFiyat().setScale(2, RoundingMode.HALF_UP);
        BigDecimal toplam = birimFiyat.multiply(BigDecimal.valueOf(adet))
                .setScale(2, RoundingMode.HALF_UP);

        // Komisyon hesabi (PRODUCT scope kurali varsa)
        Optional<CommissionRule> ruleOpt = commissionService.findProductRule(
                tenantId, staffId, product.getId(), product.getKategori());
        BigDecimal rateSnapshot = null;
        BigDecimal commissionAmount = null;
        if (ruleOpt.isPresent() && staffId != null) {
            CommissionRule rule = ruleOpt.get();
            rateSnapshot = rule.getRate();
            commissionAmount = commissionService.calculateCommissionAmount(rule, toplam);
        }

        // ProductSale kaydet (randevu NULL — standalone)
        ProductSale sale = ProductSale.builder()
                .tenantId(tenantId)
                .randevu(null)
                .product(product)
                .staffId(staffId)
                .customerId(customerId)
                .adet(adet)
                .birimFiyatSnapshot(birimFiyat)
                .toplamTutar(toplam)
                .commissionRateSnapshot(rateSnapshot)
                .commissionAmount(commissionAmount)
                .build();
        ProductSale saved = productSaleRepository.save(sale);

        // Stok dusur
        product.setStok(product.getStok() - adet);
        productRepository.save(product);

        // Earning kaydi (komisyon varsa) — randevu NULL ile
        if (ruleOpt.isPresent() && staffId != null && commissionAmount != null) {
            CommissionRule rule = ruleOpt.get();
            BigDecimal net = toplam.subtract(commissionAmount);
            Earning earning = Earning.builder()
                    .tenantId(tenantId)
                    .randevu(null)
                    .staffId(staffId)
                    .commissionRuleId(rule.getId())
                    .productSaleId(saved.getId())
                    .commissionType(rule.getCommissionType())
                    .rateSnapshot(rateSnapshot)
                    .grossAmount(toplam)
                    .commissionAmount(commissionAmount)
                    .netAmount(net)
                    .status(EarningStatus.PENDING)
                    .build();
            earningRepository.save(earning);
        }

        log.info("Standalone urun satisi: productId={}, adet={}, tutar={}, staffId={}, customerId={}",
                productId, adet, toplam, staffId, customerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductSaleResponse> getByRandevu(Long randevuId) {
        Long tenantId = TenantContext.getTenantId();
        Randevu randevu = randevuRepository.findById(randevuId)
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));
        if (!randevu.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }
        return productSaleRepository.findByRandevuId(randevuId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Long tenantId = TenantContext.getTenantId();
        ProductSale sale = productSaleRepository.findById(saleId)
                .orElseThrow(() -> ApiException.notFound("Satis bulunamadi."));
        if (!sale.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu satisa erisim yetkiniz yok.");
        }

        // Stok geri yukle
        Product product = sale.getProduct();
        product.setStok(product.getStok() + sale.getAdet());
        productRepository.save(product);

        // Iliskili earning'leri sil (PENDING ise — COLLECTED ise dokunmayalim)
        earningRepository.findAll().stream()
                .filter(e -> saleId.equals(e.getProductSaleId()) && e.getStatus() == EarningStatus.PENDING)
                .forEach(earningRepository::delete);

        Randevu randevuRef = sale.getRandevu();
        productSaleRepository.delete(sale);

        // Randevu toplamını yeniden hesapla (silinen satışı çıkar)
        if (randevuRef != null) {
            recomputeAppointmentTotal(randevuRef);
        }
    }

    /**
     * No-op — toplam fiyat hesaplaması AppointmentResponse mapping'inde yapılıyor.
     * Randevu.toplamFiyat hizmet sabiti olarak kalıyor; ürün satışları ayrı listelenir
     * ve response'da `urunToplami` + `genelToplam` field'larıyla döner.
     */
    private void recomputeAppointmentTotal(Randevu randevu) {
        // intentionally empty
    }

    @Transactional(readOnly = true)
    public List<ProductSaleResponse> getProductHistory(Long productId) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Urun bulunamadi."));
        if (!product.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu urune erisim yetkiniz yok.");
        }
        return productSaleRepository.findByProductId(tenantId, productId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductSalesSummaryResponse getSummary(LocalDate from, LocalDate to) {
        Long tenantId = TenantContext.getTenantId();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);
        List<ProductSale> sales = productSaleRepository.findByTenantIdAndPeriod(tenantId, fromDt, toDt);

        BigDecimal toplamCiro = sales.stream()
                .map(ProductSale::getToplamTutar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long toplamAdet = sales.stream().mapToLong(ProductSale::getAdet).sum();

        // Urun bazli grup
        Map<Long, List<ProductSale>> byProduct = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getProduct().getId()));
        List<ProductSalesSummaryResponse.ProductSummary> urunBazli = byProduct.entrySet().stream()
                .map(e -> {
                    Product p = e.getValue().get(0).getProduct();
                    long adet = e.getValue().stream().mapToLong(ProductSale::getAdet).sum();
                    BigDecimal ciro = e.getValue().stream()
                            .map(ProductSale::getToplamTutar)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ProductSalesSummaryResponse.ProductSummary.builder()
                            .productId(p.getId())
                            .productAd(p.getAd())
                            .adet(adet)
                            .ciro(ciro)
                            .build();
                })
                .sorted((a, b) -> b.getCiro().compareTo(a.getCiro()))
                .collect(Collectors.toList());

        // Calisan bazli grup
        Map<Long, List<ProductSale>> byStaff = sales.stream()
                .filter(s -> s.getStaffId() != null)
                .collect(Collectors.groupingBy(ProductSale::getStaffId));
        List<ProductSalesSummaryResponse.StaffSummary> calisanBazli = byStaff.entrySet().stream()
                .map(e -> {
                    Long staffId = e.getKey();
                    String adSoyad = kullaniciRepository.findById(staffId)
                            .map(k -> k.getAd() + " " + k.getSoyad())
                            .orElse("Silinmis kullanici");
                    BigDecimal ciro = e.getValue().stream()
                            .map(ProductSale::getToplamTutar)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal komisyon = e.getValue().stream()
                            .map(s -> s.getCommissionAmount() != null ? s.getCommissionAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ProductSalesSummaryResponse.StaffSummary.builder()
                            .staffId(staffId)
                            .staffAd(adSoyad)
                            .islem(e.getValue().size())
                            .ciro(ciro)
                            .toplamKomisyon(komisyon)
                            .build();
                })
                .sorted((a, b) -> b.getCiro().compareTo(a.getCiro()))
                .collect(Collectors.toList());

        return ProductSalesSummaryResponse.builder()
                .toplamCiro(toplamCiro)
                .toplamAdet(toplamAdet)
                .toplamIslem(sales.size())
                .urunBazli(urunBazli)
                .calisanBazli(calisanBazli)
                .build();
    }

    private ProductSaleResponse toResponse(ProductSale s) {
        String staffAd = null;
        if (s.getStaffId() != null) {
            staffAd = kullaniciRepository.findById(s.getStaffId())
                    .map(k -> k.getAd() + " " + k.getSoyad())
                    .orElse(null);
        }
        return ProductSaleResponse.builder()
                .id(s.getId())
                .randevuId(s.getRandevu() != null ? s.getRandevu().getId() : null)
                .productId(s.getProduct().getId())
                .productAd(s.getProduct().getAd())
                .staffId(s.getStaffId())
                .staffAd(staffAd)
                .customerId(s.getCustomerId())
                .adet(s.getAdet())
                .birimFiyatSnapshot(s.getBirimFiyatSnapshot())
                .toplamTutar(s.getToplamTutar())
                .commissionRateSnapshot(s.getCommissionRateSnapshot())
                .commissionAmount(s.getCommissionAmount())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
