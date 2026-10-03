package com.appointflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeadLetterJobResponse {
    private Long id;
    private Long originalJobId;
    private Long tenantId;
    private String jobType;
    private String payload;
    private String errorMessage;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
