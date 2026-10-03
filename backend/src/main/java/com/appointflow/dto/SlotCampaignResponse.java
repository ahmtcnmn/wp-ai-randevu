package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SlotCampaignResponse {
    private Long id;
    private Long tenantId;
    private Long cancelledRandevuId;
    private LocalDateTime slotTime;
    private Long staffId;
    private String mesaj;
    private Integer sentCount;
    private String status;
    private Long filledRandevuId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
