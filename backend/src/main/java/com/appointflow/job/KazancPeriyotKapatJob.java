package com.appointflow.job;

import com.appointflow.entity.EarningPeriod;
import com.appointflow.entity.EarningStatus;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.EarningPeriodRepository;
import com.appointflow.repository.TenantRepository;
import com.appointflow.service.CommissionService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KazancPeriyotKapatJob {

    private final TenantRepository tenantRepository;
    private final EarningPeriodRepository earningPeriodRepository;
    private final CommissionService commissionService;

    @Scheduled(cron = "0 0 1 * * *")
    public void closePeriods() {
        log.info("Kazanç periyot kapatma job'ı başlıyor...");
        List<Tenant> tenants = tenantRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Tenant tenant : tenants) {
            try {
                TenantContext.set(tenant.getId());
                processForTenant(tenant.getId(), today);
            } catch (Exception e) {
                log.error("Periyot kapatma hatası: tenantId={}, hata={}", tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Kazanç periyot kapatma job'ı tamamlandı.");
    }

    private void processForTenant(Long tenantId, LocalDate today) {
        List<EarningPeriod> pendingPeriods = earningPeriodRepository.findByTenantIdAndStatus(tenantId, EarningStatus.PENDING);

        for (EarningPeriod period : pendingPeriods) {
            if (period.getPeriodEnd().isBefore(today)) {
                try {
                    commissionService.collect(period.getId());
                    log.info("Periyot kapatıldı: periodId={}, tenantId={}", period.getId(), tenantId);
                } catch (Exception e) {
                    log.warn("Periyot kapatılamadı: periodId={}, hata={}", period.getId(), e.getMessage());
                }
            }
        }
    }
}
