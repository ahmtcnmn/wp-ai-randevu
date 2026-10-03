package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class SegmentSummaryResponse {
    private Long tenantId;
    private Integer totalCustomers;
    private Map<String, Long> segmentCounts;
    private LocalDateTime lastCalculatedAt;
}
