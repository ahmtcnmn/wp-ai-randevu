package com.appointflow.dto;

import java.util.Map;

public record CustomerReportResponse(
        Long total,
        Map<String, Long> segmentDistribution,
        Long newCustomers,
        Long returningCustomers,
        Long churnRiskCount,
        Long blacklistedCount
) {}
