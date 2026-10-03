package com.appointflow.subscription.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuotaUsageResponse {
    private long usedAppointmentsThisMonth;
    private long maxAppointmentsThisMonth;
    private long usedBranches;
    private long maxBranches;
    private long usedStaff;
    private long maxStaff;
}
