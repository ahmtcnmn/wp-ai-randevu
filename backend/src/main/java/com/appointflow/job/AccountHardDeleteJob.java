package com.appointflow.job;

import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Soft-deleted tenant'ların 30 günlük geri alma penceresi dolduğunda
 * gerçek silme işlemini yapar. Günde bir kere (her sabah 04:00) çalışır.
 *
 * Cascade: tenant_id FK olan tüm tablolarda ON DELETE CASCADE veya
 * manuel cleanup yapılmalı. Şimdilik sadece tenant + kullanıcılar silinir;
 * randevu/müşteri/abonelik gibi alt veriler ileride explicit silinmeli.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountHardDeleteJob {

    private final TenantRepository tenantRepository;
    private final KullaniciRepository kullaniciRepository;
    private final AuditService auditService;

    private static final int GRACE_PERIOD_DAYS = 30;

    /** Her gün 04:00'te kontrol et. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void hardDeleteExpiredAccounts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        List<Tenant> expired = tenantRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() != null && t.getDeletedAt().isBefore(threshold))
                .toList();

        if (expired.isEmpty()) {
            log.debug("AccountHardDeleteJob: silinecek hesap yok");
            return;
        }

        for (Tenant t : expired) {
            try {
                Long tenantId = t.getId();
                kullaniciRepository.deleteAll(kullaniciRepository.findByTenantId(tenantId));
                tenantRepository.delete(t);
                auditService.log("ACCOUNT_HARD_DELETED", "Tenant", tenantId,
                        Map.of("ad", t.getAd(), "softDeletedAt", t.getDeletedAt().toString()));
                log.warn("Hesap hard delete edildi: tenantId={}, ad={}", tenantId, t.getAd());
            } catch (Exception e) {
                log.error("AccountHardDelete basarisiz tenantId={}: {}", t.getId(), e.getMessage());
            }
        }
    }
}
