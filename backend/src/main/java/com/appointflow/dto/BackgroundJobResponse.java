package com.appointflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BackgroundJobResponse {
    private Long id;
    private Long tenantId;
    private String jobType;
    private String status;
    private String payload;
    private String idempotencyKey;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime nextRetryAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
