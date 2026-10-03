package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EarningPeriodResponse {
    private Long id;
    private Long tenantId;
    private Long staffId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Double totalGross;
    private Double totalCommission;
    private Double totalNet;
    private String status;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
}
