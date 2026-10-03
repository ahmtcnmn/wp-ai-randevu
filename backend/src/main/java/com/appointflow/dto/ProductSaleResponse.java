package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductSaleResponse {
    private Long id;
    private Long randevuId;
    private Long productId;
    private String productAd;
    private Long staffId;
    private String staffAd;
    private Long customerId;
    private Integer adet;
    private BigDecimal birimFiyatSnapshot;
    private BigDecimal toplamTutar;
    private BigDecimal commissionRateSnapshot;
    private BigDecimal commissionAmount;
    private LocalDateTime createdAt;
}
