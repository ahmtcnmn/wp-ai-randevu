package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.*;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final EarningRepository earningRepository;
    private final EarningPeriodRepository earningPeriodRepository;
    private final KullaniciRepository kullaniciRepository;

    // ─── Commission Rules ──────────────────────────────────────────────────────

    public List<CommissionRuleResponse> getRules() {
        Long tenantId = TenantContext.getTenantId();
        return commissionRuleRepository.findByTenantIdAndAktifTrue(tenantId)
                .stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    public List<CommissionRuleResponse> getRulesByScope(String scopeStr) {
        Long tenantId = TenantContext.getTenantId();
        CommissionScope scope = parseScope(scopeStr);
        return commissionRuleRepository.findByTenantIdAndScopeAndAktifTrue(tenantId, scope)
                .stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    @Transactional
    public CommissionRuleResponse createRule(CommissionRuleRequest request) {
        Long tenantId = TenantContext.getTenantId();
        CommissionType type = parseCommissionType(request.getCommissionType());
        CommissionScope scope = parseScope(request.getScope());

        // SERVICE scope ile productId/productKategori birlikte gelmemeli
        if (scope == CommissionScope.SERVICE
                && (request.getProductId() != null || request.getProductKategori() != null)) {
            throw ApiException.badRequest("SERVICE scope kurallarinda productId / productKategori belirtilemez.");
        }
        // productId VE productKategori ayni anda dolu olamaz
        if (request.getProductId() != null && request.getProductKategori() != null) {
            throw ApiException.badRequest("Bir kural ayni anda hem productId hem productKategori icin olamaz.");
        }

        // Duplicate kontrolu — ayni (staff, scope, productId, kategori) anahtarinda aktif kural varsa hata
        if (commissionRuleRepository.existsActiveRuleForKey(
                tenantId, scope, request.getStaffId(), request.getProductId(), request.getProductKategori())) {
            throw ApiException.badRequest(buildDuplicateMessage(scope, request));
        }

        CommissionRule rule = CommissionRule.builder()
                .tenantId(tenantId)
                .staffId(request.getStaffId())
                .commissionType(type)
                .scope(scope)
                .productId(request.getProductId())
                .productKategori(blankToNull(request.getProductKategori()))
                .rate(request.getRate())
                .bonusThreshold(request.getBonusThreshold())
                .aktif(request.getAktif() != null ? request.getAktif() : true)
                .build();

        return toRuleResponse(commissionRuleRepository.save(rule));
    }

    @Transactional
    public CommissionRuleResponse updateRule(Long id, CommissionRuleRequest request) {
        Long tenantId = TenantContext.getTenantId();
        CommissionRule rule = commissionRuleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Kural bulunamadi."));
        if (!rule.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu kurala erisim yetkiniz yok.");
        }

        rule.setCommissionType(parseCommissionType(request.getCommissionType()));
        rule.setRate(request.getRate());
        rule.setBonusThreshold(request.getBonusThreshold());
        if (request.getScope() != null) {
            rule.setScope(parseScope(request.getScope()));
        }
        if (request.getAktif() != null) {
            rule.setAktif(request.getAktif());
        }
        // productId/kategori update'te de degisebilir (uyarisini scope ile birlikte yap)
        rule.setProductId(request.getProductId());
        rule.setProductKategori(blankToNull(request.getProductKategori()));

        if (rule.getScope() == CommissionScope.SERVICE
                && (rule.getProductId() != null || rule.getProductKategori() != null)) {
            throw ApiException.badRequest("SERVICE scope kurallarinda productId / productKategori belirtilemez.");
        }
        if (rule.getProductId() != null && rule.getProductKategori() != null) {
            throw ApiException.badRequest("Bir kural ayni anda hem productId hem productKategori icin olamaz.");
        }

        return toRuleResponse(commissionRuleRepository.save(rule));
    }

    private CommissionScope parseScope(String scopeStr) {
        if (scopeStr == null || scopeStr.isBlank()) {
            return CommissionScope.SERVICE;
        }
        try {
            return CommissionScope.valueOf(scopeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz scope: " + scopeStr + ". Izinli: SERVICE, PRODUCT");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String buildDuplicateMessage(CommissionScope scope, CommissionRuleRequest req) {
        StringBuilder sb = new StringBuilder("Bu anahtarda zaten aktif bir kural mevcut: scope=").append(scope.name());
        sb.append(", staff=").append(req.getStaffId() == null ? "default" : req.getStaffId());
        if (req.getProductId() != null) sb.append(", product=").append(req.getProductId());
        if (req.getProductKategori() != null && !req.getProductKategori().isBlank()) {
            sb.append(", kategori=").append(req.getProductKategori());
        }
        return sb.toString();
    }

    /**
     * PRODUCT scope kuralini 4 seviyeli oncelikle bulur:
     *   1) Calisan + Belirli urun
     *   2) Calisan + Urun kategorisi
     *   3) Tenant default + Belirli urun
     *   4) Tenant default + Urun kategorisi
     *   5) Calisan genel (productId & kategori NULL)
     *   6) Tenant default genel
     * Hicbiri yoksa Optional.empty() — komisyon yok.
     */
    public Optional<CommissionRule> findProductRule(Long tenantId, Long staffId, Long productId, String kategori) {
        // 1. Calisan + Belirli urun
        if (staffId != null && productId != null) {
            Optional<CommissionRule> r = commissionRuleRepository.findStaffProductRule(tenantId, staffId, productId);
            if (r.isPresent()) return r;
        }
        // 2. Calisan + Kategori
        if (staffId != null && kategori != null && !kategori.isBlank()) {
            Optional<CommissionRule> r = commissionRuleRepository.findStaffKategoriRule(tenantId, staffId, kategori);
            if (r.isPresent()) return r;
        }
        // 3. Tenant default + Belirli urun
        if (productId != null) {
            Optional<CommissionRule> r = commissionRuleRepository.findTenantProductRule(tenantId, productId);
            if (r.isPresent()) return r;
        }
        // 4. Tenant default + Kategori
        if (kategori != null && !kategori.isBlank()) {
            Optional<CommissionRule> r = commissionRuleRepository.findTenantKategoriRule(tenantId, kategori);
            if (r.isPresent()) return r;
        }
        // 5. Calisan genel (her urun icin)
        if (staffId != null) {
            Optional<CommissionRule> r = commissionRuleRepository.findStaffGeneralRule(
                    tenantId, staffId, CommissionScope.PRODUCT);
            if (r.isPresent()) return r;
        }
        // 6. Tenant default genel
        return commissionRuleRepository.findTenantDefaultRule(tenantId, CommissionScope.PRODUCT);
    }

    public BigDecimal calculateCommissionAmount(CommissionRule rule, BigDecimal gross) {
        return calculateCommission(rule, gross);
    }

    @Transactional
    public void deleteRule(Long id) {
        Long tenantId = TenantContext.getTenantId();
        CommissionRule rule = commissionRuleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Kural bulunamadi."));
        if (!rule.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu kurala erisim yetkiniz yok.");
        }
        rule.setAktif(false);
        commissionRuleRepository.save(rule);
    }

    // ─── Earning Hook ──────────────────────────────────────────────────────────

    @Transactional
    public void createEarningForAppointment(Randevu randevu) {
        Long tenantId = randevu.getTenantId();
        Long staffId = randevu.getUzman().getId();

        // Prevent duplicate earning for same appointment
        if (earningRepository.existsByRandevuId(randevu.getId())) {
            return;
        }

        // 1. Staff-specific SERVICE rule first
        Optional<CommissionRule> ruleOpt = commissionRuleRepository
                .findStaffGeneralRule(tenantId, staffId, CommissionScope.SERVICE);

        // 2. Fallback to tenant default SERVICE rule
        if (ruleOpt.isEmpty()) {
            ruleOpt = commissionRuleRepository.findTenantDefaultRule(tenantId, CommissionScope.SERVICE);
        }

        // 3. No rule → silent exit
        if (ruleOpt.isEmpty()) {
            log.debug("Komisyon kurali bulunamadi: tenantId={}, staffId={}", tenantId, staffId);
            return;
        }

        CommissionRule rule = ruleOpt.get();
        BigDecimal grossAmount = randevu.getToplamFiyat() != null
                ? BigDecimal.valueOf(randevu.getToplamFiyat()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal commissionAmount = calculateCommission(rule, grossAmount);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount);

        Earning earning = Earning.builder()
                .tenantId(tenantId)
                .randevu(randevu)
                .staffId(staffId)
                .commissionRuleId(rule.getId())
                .commissionType(rule.getCommissionType())
                .rateSnapshot(rule.getRate())
                .grossAmount(grossAmount)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .status(EarningStatus.PENDING)
                .build();

        earningRepository.save(earning);
        log.debug("Earning olusturuldu: randevuId={}, staffId={}, commission={}", randevu.getId(), staffId, commissionAmount);
    }

    // ─── Earning Periods ───────────────────────────────────────────────────────

    public List<EarningPeriodResponse> getPeriods() {
        Long tenantId = TenantContext.getTenantId();
        return earningPeriodRepository.findByTenantId(tenantId)
                .stream().map(this::toPeriodResponse).collect(Collectors.toList());
    }

    @Transactional
    public EarningPeriodResponse createPeriod(EarningPeriodRequest request) {
        Long tenantId = TenantContext.getTenantId();

        // Check for duplicate period
        earningPeriodRepository.findByTenantIdAndStaffIdAndPeriodStartAndPeriodEnd(
                tenantId, request.getStaffId(), request.getPeriodStart(), request.getPeriodEnd())
                .ifPresent(p -> {
                    throw ApiException.badRequest("Bu donem icin zaten bir kayit mevcut.");
                });

        EarningPeriod period = EarningPeriod.builder()
                .tenantId(tenantId)
                .staffId(request.getStaffId())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .build();

        return toPeriodResponse(earningPeriodRepository.save(period));
    }

    @Transactional
    public EarningPeriodResponse collect(Long periodId) {
        Long tenantId = TenantContext.getTenantId();
        EarningPeriod period = earningPeriodRepository.findById(periodId)
                .orElseThrow(() -> ApiException.notFound("Donem bulunamadi."));
        if (!period.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu doneme erisim yetkiniz yok.");
        }
        if (period.getStatus() == EarningStatus.COLLECTED) {
            throw ApiException.badRequest("Bu donem zaten tahsil edilmis.");
        }

        // Find PENDING earnings in the date range and assign to period
        List<Earning> pendingEarnings = earningRepository.findPendingForPeriod(
                tenantId, period.getStaffId(), period.getPeriodStart(), period.getPeriodEnd());

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Earning e : pendingEarnings) {
            e.setStatus(EarningStatus.COLLECTED);
            e.setEarningPeriodId(period.getId());
            earningRepository.save(e);
            totalGross = totalGross.add(e.getGrossAmount());
            totalCommission = totalCommission.add(e.getCommissionAmount());
            totalNet = totalNet.add(e.getNetAmount());
        }

        period.setTotalGross(totalGross);
        period.setTotalCommission(totalCommission);
        period.setTotalNet(totalNet);
        period.setStatus(EarningStatus.COLLECTED);
        period.setCollectedAt(LocalDateTime.now());

        return toPeriodResponse(earningPeriodRepository.save(period));
    }

    public List<EarningResponse> getPeriodEarnings(Long periodId) {
        Long tenantId = TenantContext.getTenantId();
        EarningPeriod period = earningPeriodRepository.findById(periodId)
                .orElseThrow(() -> ApiException.notFound("Donem bulunamadi."));
        if (!period.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu doneme erisim yetkiniz yok.");
        }
        return earningRepository.findByEarningPeriodId(periodId)
                .stream().map(this::toEarningResponse).collect(Collectors.toList());
    }

    // ─── Staff Earnings ────────────────────────────────────────────────────────

    public List<EarningResponse> getMyEarnings(String email) {
        Long tenantId = TenantContext.getTenantId();
        Kullanici staff = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return earningRepository.findByTenantIdAndStaffId(tenantId, staff.getId())
                .stream().map(this::toEarningResponse).collect(Collectors.toList());
    }

    public EarningsSummaryResponse getMySummary(String email) {
        Long tenantId = TenantContext.getTenantId();
        Kullanici staff = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return buildSummary(tenantId, staff.getId());
    }

    public List<EarningResponse> getStaffEarnings(Long staffId) {
        Long tenantId = TenantContext.getTenantId();
        return earningRepository.findByTenantIdAndStaffId(tenantId, staffId)
                .stream().map(this::toEarningResponse).collect(Collectors.toList());
    }

    public EarningsSummaryResponse getStaffSummary(Long staffId) {
        Long tenantId = TenantContext.getTenantId();
        return buildSummary(tenantId, staffId);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private EarningsSummaryResponse buildSummary(Long tenantId, Long staffId) {
        List<Earning> all = earningRepository.findByTenantIdAndStaffId(tenantId, staffId);
        BigDecimal totalGross = all.stream().map(Earning::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommission = all.stream().map(Earning::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingCommission = earningRepository.sumCommissionByStaffAndStatus(tenantId, staffId, EarningStatus.PENDING);
        BigDecimal collectedCommission = earningRepository.sumCommissionByStaffAndStatus(tenantId, staffId, EarningStatus.COLLECTED);

        long pending = all.stream().filter(e -> e.getStatus() == EarningStatus.PENDING).count();
        long collected = all.stream().filter(e -> e.getStatus() == EarningStatus.COLLECTED).count();

        return EarningsSummaryResponse.builder()
                .staffId(staffId)
                .totalGross(totalGross.doubleValue())
                .totalCommission(totalCommission.doubleValue())
                .pendingCommission(pendingCommission != null ? pendingCommission.doubleValue() : 0.0)
                .collectedCommission(collectedCommission != null ? collectedCommission.doubleValue() : 0.0)
                .totalEarnings((long) all.size())
                .pendingEarnings(pending)
                .collectedEarnings(collected)
                .build();
    }

    private BigDecimal calculateCommission(CommissionRule rule, BigDecimal gross) {
        return switch (rule.getCommissionType()) {
            case PERCENTAGE -> gross.multiply(rule.getRate()).setScale(2, RoundingMode.HALF_UP);
            case SALARY_PLUS_BONUS -> {
                if (rule.getBonusThreshold() != null && gross.compareTo(rule.getBonusThreshold()) >= 0) {
                    yield gross.multiply(rule.getRate()).setScale(2, RoundingMode.HALF_UP);
                }
                yield BigDecimal.ZERO;
            }
        };
    }

    private CommissionType parseCommissionType(String type) {
        try {
            return CommissionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz komisyon tipi: " + type);
        }
    }

    private CommissionRuleResponse toRuleResponse(CommissionRule r) {
        return CommissionRuleResponse.builder()
                .id(r.getId())
                .tenantId(r.getTenantId())
                .staffId(r.getStaffId())
                .commissionType(r.getCommissionType().name())
                .rate(r.getRate())
                .bonusThreshold(r.getBonusThreshold())
                .aktif(r.getAktif())
                .scope(r.getScope() != null ? r.getScope().name() : "SERVICE")
                .productId(r.getProductId())
                .productKategori(r.getProductKategori())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private EarningResponse toEarningResponse(Earning e) {
        return EarningResponse.builder()
                .id(e.getId())
                .randevuId(e.getRandevu().getId())
                .staffId(e.getStaffId())
                .commissionRuleId(e.getCommissionRuleId())
                .earningPeriodId(e.getEarningPeriodId())
                .commissionType(e.getCommissionType().name())
                .rateSnapshot(e.getRateSnapshot().doubleValue())
                .grossAmount(e.getGrossAmount().doubleValue())
                .commissionAmount(e.getCommissionAmount().doubleValue())
                .netAmount(e.getNetAmount().doubleValue())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EarningPeriodResponse toPeriodResponse(EarningPeriod p) {
        return EarningPeriodResponse.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .staffId(p.getStaffId())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .totalGross(p.getTotalGross().doubleValue())
                .totalCommission(p.getTotalCommission().doubleValue())
                .totalNet(p.getTotalNet().doubleValue())
                .status(p.getStatus().name())
                .collectedAt(p.getCollectedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
