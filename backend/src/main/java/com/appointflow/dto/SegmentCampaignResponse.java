package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SegmentCampaignResponse {
    private Long id;
    private Long tenantId;
    private String baslik;
    private String hedefSegment;
    private String mesaj;
    private Integer sentCount;
    private Integer responseCount;
    private Double conversionRate; // responseCount / sentCount * 100
    private Long createdById;
    private String status;
    private LocalDateTime createdAt;
}
