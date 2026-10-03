package com.appointflow.subscription.dto;

import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {
    private Long id;
    private SubscriptionPlanResponse plan;
    private String status;
    private String baslangicTarihi;
    private String denemeBitisTarihi;
    private String sonrakiOdemeTarihi;
    private String gracePeriodBitis;

    /** Trial sırasında kalan gün sayısı. Trial değilse veya bittiyse null. */
    private Integer trialDaysRemaining;

    /** Trial durumda mı (UI banner için). */
    private Boolean isTrialing;

    /** Hesap kullanılabilir mi (TRIALING veya ACTIVE ise true). EXPIRED/SUSPENDED → false. */
    private Boolean isUsable;

    public static SubscriptionResponse from(Subscription s) {
        Integer remaining = null;
        boolean trialing = s.getStatus() == SubscriptionStatus.TRIALING;
        if (trialing && s.getDenemeBitisTarihi() != null) {
            long days = Duration.between(LocalDateTime.now(), s.getDenemeBitisTarihi()).toDays();
            remaining = (int) Math.max(0, days);
        }
        boolean usable = s.getStatus() == SubscriptionStatus.ACTIVE
                || s.getStatus() == SubscriptionStatus.TRIALING;

        return SubscriptionResponse.builder()
                .id(s.getId())
                .plan(SubscriptionPlanResponse.from(s.getPlan()))
                .status(s.getStatus().name())
                .baslangicTarihi(s.getBaslangicTarihi() != null ? s.getBaslangicTarihi().toString() : null)
                .denemeBitisTarihi(s.getDenemeBitisTarihi() != null ? s.getDenemeBitisTarihi().toString() : null)
                .sonrakiOdemeTarihi(s.getSonrakiOdemeTarihi() != null ? s.getSonrakiOdemeTarihi().toString() : null)
                .gracePeriodBitis(s.getGracePeriodBitis() != null ? s.getGracePeriodBitis().toString() : null)
                .trialDaysRemaining(remaining)
                .isTrialing(trialing)
                .isUsable(usable)
                .build();
    }
}
