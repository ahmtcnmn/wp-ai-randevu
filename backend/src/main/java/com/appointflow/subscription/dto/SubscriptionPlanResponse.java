package com.appointflow.subscription.dto;

import com.appointflow.subscription.entity.SubscriptionPlan;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SubscriptionPlanResponse {
    private Long id;
    private String planKey;
    private String ad;
    private String aciklama;
    private BigDecimal aylikFiyat;
    private Integer maxSube;
    private Integer maxCalisan;
    private Integer maxAylikRandevu;
    private Boolean aktif;

    public static SubscriptionPlanResponse from(SubscriptionPlan p) {
        return SubscriptionPlanResponse.builder()
                .id(p.getId())
                .planKey(p.getPlanKey())
                .ad(p.getAd())
                .aciklama(p.getAciklama())
                .aylikFiyat(p.getAylikFiyat())
                .maxSube(p.getMaxSube())
                .maxCalisan(p.getMaxCalisan())
                .maxAylikRandevu(p.getMaxAylikRandevu())
                .aktif(p.getAktif())
                .build();
    }
}
