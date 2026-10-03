package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ProductRequest;
import com.appointflow.dto.ProductResponse;
import com.appointflow.entity.Hizmet;
import com.appointflow.entity.Product;
import com.appointflow.entity.ProductServiceSuggestion;
import com.appointflow.repository.HizmetRepository;
import com.appointflow.repository.ProductRepository;
import com.appointflow.repository.ProductServiceSuggestionRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductServiceSuggestionRepository suggestionRepository;
    private final HizmetRepository hizmetRepository;

    public List<ProductResponse> getAll() {
        return productRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Belirli bir hizmet için önerilen aktif ürünler.
     * Randevu detayında "Bu randevuya önerilen ürünler" kartında kullanılır.
     * Sadece aiOneriAktif=true ve aktif=true ürünler döner.
     */
    public List<ProductResponse> getRecommendedForHizmet(Long hizmetId) {
        Long tenantId = TenantContext.getTenantId();
        return suggestionRepository.findByHizmetIdAndProductActive(hizmetId).stream()
                .map(ProductServiceSuggestion::getProduct)
                .filter(p -> p.getTenantId().equals(tenantId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getById(Long id) {
        return toResponse(findByIdAndTenant(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .tenantId(TenantContext.getTenantId())
                .ad(request.getAd())
                .aciklama(request.getAciklama())
                .fiyat(request.getFiyat())
                .stok(request.getStok() != null ? request.getStok() : 0)
                .kategori(request.getKategori())
                .aiOneriAktif(request.getAiOneriAktif() != null ? request.getAiOneriAktif() : false)
                .build();

        productRepository.save(product);
        saveServiceSuggestions(product, request.getHizmetIds());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findByIdAndTenant(id);
        product.setAd(request.getAd());
        product.setAciklama(request.getAciklama());
        product.setFiyat(request.getFiyat());
        if (request.getStok() != null) product.setStok(request.getStok());
        if (request.getKategori() != null) product.setKategori(request.getKategori());
        if (request.getAiOneriAktif() != null) product.setAiOneriAktif(request.getAiOneriAktif());

        productRepository.save(product);

        suggestionRepository.deleteByProductId(id);
        saveServiceSuggestions(product, request.getHizmetIds());

        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findByIdAndTenant(id);
        product.setAktif(false);
        productRepository.save(product);
    }

    private void saveServiceSuggestions(Product product, List<Long> hizmetIds) {
        if (hizmetIds == null || hizmetIds.isEmpty()) return;
        for (Long hizmetId : hizmetIds) {
            Hizmet hizmet = hizmetRepository.findById(hizmetId)
                    .orElseThrow(() -> ApiException.notFound("Hizmet bulunamadi: " + hizmetId));
            if (!hizmet.getTenantId().equals(TenantContext.getTenantId())) {
                throw ApiException.forbidden("Bu hizmete erisim yetkiniz yok.");
            }
            ProductServiceSuggestion suggestion = ProductServiceSuggestion.builder()
                    .product(product)
                    .hizmet(hizmet)
                    .build();
            suggestionRepository.save(suggestion);
        }
    }

    private Product findByIdAndTenant(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Urun bulunamadi."));
        if (!p.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu urune erisim yetkiniz yok.");
        }
        return p;
    }

    private ProductResponse toResponse(Product p) {
        List<Long> hizmetIds = suggestionRepository.findByProductId(p.getId()).stream()
                .map(s -> s.getHizmet().getId())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(p.getId())
                .ad(p.getAd())
                .aciklama(p.getAciklama())
                .fiyat(p.getFiyat())
                .stok(p.getStok())
                .kategori(p.getKategori())
                .aiOneriAktif(p.getAiOneriAktif())
                .aktif(p.getAktif())
                .hizmetIds(hizmetIds)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
