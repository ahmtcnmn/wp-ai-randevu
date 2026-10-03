package com.appointflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CommissionRuleRequest {

    private Long staffId; // null = tenant default

    @NotBlank
    private String commissionType; // PERCENTAGE or SALARY_PLUS_BONUS

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal rate;

    private BigDecimal bonusThreshold;

    private Boolean aktif;

    /** SERVICE (default) veya PRODUCT — geriye uyum icin opsiyonel */
    private String scope;

    /** Sadece scope=PRODUCT ile birlikte gecerli — null = tum urunler */
    private Long productId;

    /** Sadece scope=PRODUCT ile birlikte gecerli — null = tum kategoriler */
    private String productKategori;
}
