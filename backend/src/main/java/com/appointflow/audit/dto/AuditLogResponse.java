package com.appointflow.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String userEmail;
    private String userRol;
    private String action;
    private String entityType;
    private Long entityId;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
