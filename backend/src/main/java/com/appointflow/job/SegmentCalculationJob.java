package com.appointflow.job;

import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import com.appointflow.service.SegmentationService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentCalculationJob {

    private final TenantRepository tenantRepository;
    private final SegmentationService segmentationService;

    // Her gece 02:00'da çalışır
    @Scheduled(cron = "0 0 2 * * *")
    public void calculateSegments() {
        log.info("Segment hesaplama job'i basliyor...");
        List<Tenant> tenants = tenantRepository.findAll();

        for (Tenant tenant : tenants) {
            try {
                TenantContext.set(tenant.getId());
                segmentationService.calculateForTenant(tenant.getId());
            } catch (Exception e) {
                log.error("Segment hesaplama hatasi: tenantId={}, hata={}", tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Segment hesaplama job'i tamamlandi. Toplam tenant: {}", tenants.size());
    }
}
