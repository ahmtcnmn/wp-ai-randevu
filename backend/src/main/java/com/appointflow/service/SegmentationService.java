package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.CustomerSegmentResponse;
import com.appointflow.dto.SegmentSummaryResponse;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentationService {

    private final CustomerRepository customerRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final RandevuRepository randevuRepository;

    // ─── Public API ────────────────────────────────────────────────────────────

    public SegmentSummaryResponse getSummary() {
        Long tenantId = TenantContext.getTenantId();
        List<Object[]> counts = customerSegmentRepository.countBySegmentType(tenantId);
        int total = customerRepository.findByTenantId(tenantId).size();

        Map<String, Long> segmentCounts = new LinkedHashMap<>();
        // Initialize all segments with 0
        for (SegmentType st : SegmentType.values()) {
            segmentCounts.put(st.name(), 0L);
        }
        for (Object[] row : counts) {
            segmentCounts.put(((SegmentType) row[0]).name(), (Long) row[1]);
        }

        LocalDateTime lastCalculated = customerSegmentRepository.findByTenantId(tenantId)
                .stream()
                .map(CustomerSegment::getCalculatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return SegmentSummaryResponse.builder()
                .tenantId(tenantId)
                .totalCustomers(total)
                .segmentCounts(segmentCounts)
                .lastCalculatedAt(lastCalculated)
                .build();
    }

    public List<CustomerSegmentResponse> getBySegmentType(String segmentTypeStr) {
        Long tenantId = TenantContext.getTenantId();
        SegmentType segmentType = parseSegmentType(segmentTypeStr);
        return customerSegmentRepository.findByTenantIdAndSegmentType(tenantId, segmentType)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CustomerSegmentResponse getCustomerSegment(Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        CustomerSegment seg = customerSegmentRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseThrow(() -> ApiException.notFound("Bu musteri icin segment henuz hesaplanmamis."));
        return toResponse(seg);
    }

    // ─── Calculation ───────────────────────────────────────────────────────────

    @Transactional
    public void calculateForTenant(Long tenantId) {
        log.info("Segment hesaplama basliyor: tenantId={}", tenantId);

        List<Customer> customers = customerRepository.findByTenantId(tenantId);
        Double tenantAvgSpend = randevuRepository.avgToplamFiyatByTenant(tenantId);
        if (tenantAvgSpend == null) tenantAvgSpend = 0.0;

        for (Customer customer : customers) {
            try {
                recalculate(tenantId, customer, tenantAvgSpend);
            } catch (Exception e) {
                log.warn("Segment hesaplanamadi: customerId={}, hata={}", customer.getId(), e.getMessage());
            }
        }

        log.info("Segment hesaplama tamamlandi: tenantId={}, musteri={}", tenantId, customers.size());
    }

    @Transactional
    public CustomerSegmentResponse recalculateForCustomer(Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ApiException.notFound("Musteri bulunamadi."));
        if (!customer.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu musteriye erisim yetkiniz yok.");
        }
        Double tenantAvgSpend = randevuRepository.avgToplamFiyatByTenant(tenantId);
        if (tenantAvgSpend == null) tenantAvgSpend = 0.0;

        CustomerSegment seg = recalculate(tenantId, customer, tenantAvgSpend);
        return toResponse(seg);
    }

    // ─── Core Algorithm ────────────────────────────────────────────────────────

    private CustomerSegment recalculate(Long tenantId, Customer customer, double tenantAvgSpend) {
        List<Randevu> allAppointments = randevuRepository.findByTenantIdAndCustomerId(tenantId, customer.getId());
        List<Randevu> completed = allAppointments.stream()
                .filter(r -> r.getDurum() == RandevuDurumu.TAMAMLANDI)
                .sorted(Comparator.comparing(Randevu::getTarihSaat).reversed())
                .collect(Collectors.toList());
        List<Randevu> noShows = allAppointments.stream()
                .filter(r -> r.getDurum() == RandevuDurumu.GELMEDI)
                .collect(Collectors.toList());

        int totalAppointments = completed.size();
        double totalSpent = completed.stream()
                .mapToDouble(r -> r.getToplamFiyat() != null ? r.getToplamFiyat() : 0.0)
                .sum();
        double avgSpent = totalAppointments > 0 ? totalSpent / totalAppointments : 0.0;
        int noShowCount = noShows.size();

        Integer daysSinceLastVisit = null;
        if (customer.getSonZiyaret() != null) {
            daysSinceLastVisit = (int) ChronoUnit.DAYS.between(customer.getSonZiyaret(), LocalDateTime.now());
        } else if (!completed.isEmpty()) {
            daysSinceLastVisit = (int) ChronoUnit.DAYS.between(
                    completed.get(0).getTarihSaat(), LocalDateTime.now());
        }

        int customerAgeDays = (int) ChronoUnit.DAYS.between(customer.getCreatedAt(), LocalDateTime.now());

        SegmentType segmentType = determineSegment(
                totalAppointments, totalSpent, avgSpent, noShowCount,
                daysSinceLastVisit, customerAgeDays, tenantAvgSpend, completed, noShows);

        // Upsert
        CustomerSegment seg = customerSegmentRepository
                .findByTenantIdAndCustomerId(tenantId, customer.getId())
                .orElse(CustomerSegment.builder()
                        .tenantId(tenantId)
                        .customer(customer)
                        .build());

        seg.setSegmentType(segmentType);
        seg.setTotalAppointments(totalAppointments);
        seg.setTotalSpent(java.math.BigDecimal.valueOf(totalSpent).setScale(2, java.math.RoundingMode.HALF_UP));
        seg.setAvgSpentPerVisit(java.math.BigDecimal.valueOf(avgSpent).setScale(2, java.math.RoundingMode.HALF_UP));
        seg.setDaysSinceLastVisit(daysSinceLastVisit);
        seg.setNoShowCount(noShowCount);
        seg.setCalculatedAt(LocalDateTime.now());

        return customerSegmentRepository.save(seg);
    }

    private SegmentType determineSegment(int totalAppointments, double totalSpent,
                                          double avgSpent, int noShowCount,
                                          Integer daysSinceLastVisit, int customerAgeDays,
                                          double tenantAvgSpend,
                                          List<Randevu> completed, List<Randevu> noShows) {
        // AT_RISK: Gelmeme sayısı ≥ 2 veya son 3 randevudan 2'si GELMEDI
        if (noShowCount >= 2) return SegmentType.AT_RISK;
        if (noShows.size() >= 2) {
            // Son 3 randevudan 2'si gelmedi mi?
            // noShows listesi tüm gelmedi'leri içeriyor, basit kontrol yeterli
            return SegmentType.AT_RISK;
        }

        // LOST: 120+ gün hiç gelmemiş
        if (daysSinceLastVisit != null && daysSinceLastVisit >= 120) return SegmentType.LOST;
        if (daysSinceLastVisit == null && customerAgeDays >= 30) return SegmentType.LOST;

        // NEW: İlk randevusu 30 gün içinde veya toplam ≤ 2 randevu ve müşteri yaşı ≤ 30 gün
        if (totalAppointments <= 2 && customerAgeDays <= 30) return SegmentType.NEW;

        // VIP: Ortalama harcama ≥ tenant ortalaması × 2
        if (tenantAvgSpend > 0 && avgSpent >= tenantAvgSpend * 2) return SegmentType.VIP;

        // LOYAL: 6+ ay müşteri, 10+ randevu veya sadakat puanı ≥ 500 (puan verisi burada yok, randevu sayısı yeterli)
        if (customerAgeDays >= 180 && totalAppointments >= 10) return SegmentType.LOYAL;

        // DRIFTING: Son 60-120 gün arasında sessiz
        if (daysSinceLastVisit != null && daysSinceLastVisit >= 60 && daysSinceLastVisit < 120) {
            return SegmentType.DRIFTING;
        }

        // REGULAR: Son 60 günde aktif, ayda en az 1 randevu
        if (daysSinceLastVisit != null && daysSinceLastVisit < 60) {
            // Son 30 günde randevu var mı?
            boolean activeLastMonth = completed.stream()
                    .anyMatch(r -> ChronoUnit.DAYS.between(r.getTarihSaat(), LocalDateTime.now()) <= 30);
            if (activeLastMonth) return SegmentType.REGULAR;
        }

        // OCCASIONAL: 60-120 günde bir randevu
        return SegmentType.OCCASIONAL;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private SegmentType parseSegmentType(String type) {
        try {
            return SegmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz segment tipi: " + type +
                    ". Gecerli degerler: NEW, REGULAR, LOYAL, OCCASIONAL, DRIFTING, LOST, VIP, AT_RISK");
        }
    }

    private CustomerSegmentResponse toResponse(CustomerSegment cs) {
        Customer c = cs.getCustomer();
        return CustomerSegmentResponse.builder()
                .customerId(c.getId())
                .musteriAd(c.getAd() + " " + c.getSoyad())
                .telefon(c.getTelefon())
                .segmentType(cs.getSegmentType().name())
                .totalAppointments(cs.getTotalAppointments())
                .totalSpent(cs.getTotalSpent() != null ? cs.getTotalSpent().doubleValue() : null)
                .avgSpentPerVisit(cs.getAvgSpentPerVisit() != null ? cs.getAvgSpentPerVisit().doubleValue() : null)
                .daysSinceLastVisit(cs.getDaysSinceLastVisit())
                .noShowCount(cs.getNoShowCount())
                .calculatedAt(cs.getCalculatedAt())
                .build();
    }
}
