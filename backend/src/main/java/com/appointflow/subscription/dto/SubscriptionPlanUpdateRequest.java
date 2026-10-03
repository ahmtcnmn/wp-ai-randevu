package com.appointflow.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SubscriptionPlanUpdateRequest {
    @NotBlank
    private String ad;
    private String aciklama;
    @DecimalMin("0.01")
    private BigDecimal aylikFiyat;
    private Integer maxSube;
    private Integer maxCalisan;
    private Integer maxAylikRandevu;
    private String iyzicoReferenceCode;
    private Boolean aktif;
}
