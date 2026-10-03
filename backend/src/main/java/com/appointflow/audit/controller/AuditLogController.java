package com.appointflow.audit.controller;

import com.appointflow.audit.AuditActionLabels;
import com.appointflow.audit.dto.AuditLogResponse;
import com.appointflow.audit.entity.AuditLog;
import com.appointflow.audit.repository.AuditLogRepository;
import com.appointflow.common.ApiResponse;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action) {
        Long tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> result = (action != null && !action.isBlank())
                ? auditLogRepository.findByTenantIdAndAction(tenantId, action.toUpperCase(), pageable)
                : auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.map(this::toResponse)));
    }

    /**
     * Tüm desteklenen action enum'larının Türkçe etiket map'i.
     * Frontend bunu cache'leyip listede action'ları human-readable gösterir.
     */
    @GetMapping("/actions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getActionLabels() {
        return ResponseEntity.ok(ApiResponse.success(AuditActionLabels.all()));
    }

    @GetMapping("/entity/{type}/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getForEntity(
            @PathVariable("type") String entityType,
            @PathVariable("id") Long entityId) {
        Long tenantId = TenantContext.getTenantId();
        List<AuditLog> result = auditLogRepository.findByEntity(tenantId, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(result.stream().map(this::toResponse).toList()));
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .tenantId(a.getTenantId())
                .userId(a.getUserId())
                .userEmail(a.getUserEmail())
                .userRol(a.getUserRol())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .details(a.getDetails())
                .ipAddress(a.getIpAddress())
                .userAgent(a.getUserAgent())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
