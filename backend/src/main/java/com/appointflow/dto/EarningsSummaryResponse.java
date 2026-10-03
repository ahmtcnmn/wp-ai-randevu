package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EarningsSummaryResponse {
    private Long staffId;
    private Double totalGross;
    private Double totalCommission;
    private Double pendingCommission;
    private Double collectedCommission;
    private Long totalEarnings;
    private Long pendingEarnings;
    private Long collectedEarnings;
}
