package com.appointflow.job;

import com.appointflow.repository.RandevuRepository;
import com.appointflow.repository.TenantRepository;
import com.appointflow.subscription.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaReconciliationJob {

    private final TenantRepository tenantRepository;
    private final RandevuRepository randevuRepository;
    private final QuotaService quotaService;

    @Scheduled(cron = "0 30 3 * * *")
    public void reconcile() {
        var tenants = tenantRepository.findAll();
        log.info("QuotaReconciliationJob: {} tenant işlenecek", tenants.size());

        YearMonth ym = YearMonth.now();
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);

        for (var tenant : tenants) {
            try {
                long actualCount = randevuRepository.countByTenantIdAndPeriod(tenant.getId(), from, to);
                quotaService.reconcileCounter(tenant.getId(), (int) actualCount);
            } catch (Exception e) {
                log.warn("Quota reconcile hatası: tenantId={}, hata={}", tenant.getId(), e.getMessage());
            }
        }
        log.info("QuotaReconciliationJob tamamlandı.");
    }
}
