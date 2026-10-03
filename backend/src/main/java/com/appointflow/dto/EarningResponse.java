package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EarningResponse {
    private Long id;
    private Long randevuId;
    private Long staffId;
    private Long commissionRuleId;
    private Long earningPeriodId;
    private String commissionType;
    private Double rateSnapshot;
    private Double grossAmount;
    private Double commissionAmount;
    private Double netAmount;
    private String status;
    private LocalDateTime createdAt;
}
