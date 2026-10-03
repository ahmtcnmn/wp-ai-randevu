package com.appointflow.dto;

import java.util.Map;

public record CampaignReportResponse(
        Long slotCampaignsTotal,
        Long slotCampaignsFilled,
        Double slotFillRate,
        Long segmentCampaignsTotal,
        Double segmentConversionRate,
        Map<String, Long> slotByStatus
) {}
