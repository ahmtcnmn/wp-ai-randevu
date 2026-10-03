package com.appointflow.subscription.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTenantResponse {
    private Long id;
    private String ad;
    private String slug;
    private String email;
    private String telefon;
    private Boolean aktif;
    private String createdAt;
    private String planKey;
    private String subscriptionStatus;
    private String subscriptionEnd;
    /** Sektör — BARBER, HAIR_SALON, DENTAL_CLINIC, ... */
    private String businessType;
}
