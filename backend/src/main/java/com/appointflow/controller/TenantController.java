package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.CancellationPolicyRequest;
import com.appointflow.dto.CancellationPolicyResponse;
import com.appointflow.dto.TenantResponse;
import com.appointflow.dto.TenantUpdateRequest;
import com.appointflow.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getCurrentTenant()));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(@Valid @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.updateTenant(request)));
    }

    @PutMapping("/onboarding-complete")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<TenantResponse>> completeOnboarding() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.completeOnboarding()));
    }

    @GetMapping("/cancellation-policy")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> getCancellationPolicy() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getCancellationPolicy()));
    }

    @PutMapping("/cancellation-policy")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> updateCancellationPolicy(
            @Valid @RequestBody CancellationPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.updateCancellationPolicy(request)));
    }
}
