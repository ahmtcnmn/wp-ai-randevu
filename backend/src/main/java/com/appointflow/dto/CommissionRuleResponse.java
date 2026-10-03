package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommissionRuleResponse {
    private Long id;
    private Long tenantId;
    private Long staffId;
    private String commissionType;
    private BigDecimal rate;
    private BigDecimal bonusThreshold;
    private Boolean aktif;
    private String scope;
    private Long productId;
    private String productKategori;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
