package com.appointflow.job;

import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.SubscriptionRepository;
import com.appointflow.subscription.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryJob {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService planService;

    /**
     * Her sabah 06:00 — grace period bitmiş PAST_DUE abonelikleri SUSPENDED yap.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void suspendExpiredGracePeriods() {
        var expired = subscriptionRepository.findExpiredGracePeriod(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("SubscriptionExpiryJob: {} abonelik askıya alınacak", expired.size());
        for (var sub : expired) {
            sub.setStatus(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(sub);
            try {
                planService.disableAllPlanFeatures(sub.getTenantId(), sub.getPlan().getPlanKey());
            } catch (Exception e) {
                log.warn("Feature deaktive hatası: tenantId={}, hata={}", sub.getTenantId(), e.getMessage());
            }
            log.info("Abonelik askıya alındı: tenantId={}, subscriptionId={}", sub.getTenantId(), sub.getId());
        }
    }

    /**
     * Her saat başı — 14 günlük trial süresi dolan abonelikleri EXPIRED yap.
     * Kullanıcı sonraki login/istekte 402 alır, /finans'a yönlendirilir.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireFinishedTrials() {
        var expired = subscriptionRepository.findExpiredTrials(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("SubscriptionExpiryJob: {} trial süresi doldu, EXPIRED'a alınıyor", expired.size());
        for (var sub : expired) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            try {
                planService.disableAllPlanFeatures(sub.getTenantId(), sub.getPlan().getPlanKey());
            } catch (Exception e) {
                log.warn("Feature deaktive hatası (trial expire): tenantId={}, hata={}",
                        sub.getTenantId(), e.getMessage());
            }
            log.info("Trial sona erdi: tenantId={}, subscriptionId={}, denemeBitis={}",
                    sub.getTenantId(), sub.getId(), sub.getDenemeBitisTarihi());
        }
    }
}
