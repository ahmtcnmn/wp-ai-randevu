package com.appointflow.subscription.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.subscription.dto.*;
import com.appointflow.subscription.service.BillingService;
import com.appointflow.subscription.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPlanController {

    private final SubscriptionPlanService planService;
    private final BillingService billingService;
    private final com.appointflow.service.AccountDeletionService accountDeletionService;

    @GetMapping("/plans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllPlans()));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(planService.updatePlan(id, request)));
    }

    @PostMapping("/plans/{id}/features")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setPlanFeatures(
            @PathVariable Long id,
            @RequestBody PlanFeaturesRequest request) {
        planService.applyPlanFeatures(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminTenantResponse>>> getAllTenants() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getAllTenants()));
    }

    /**
     * Tenant detayı — bilgi + abonelik + (placeholder) son aktivite.
     * Detay sayfasında plan override / sektör değiştir / aktif değiştir butonları.
     */
    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminTenantResponse>> getTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(billingService.getTenantDetail(tenantId)));
    }

    @GetMapping("/tenants/{tenantId}/subscription")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getTenantSubscription(
            @PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(billingService.getSubscription(tenantId)));
    }

    @PostMapping("/tenants/{tenantId}/override-plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> overridePlan(
            @PathVariable Long tenantId,
            @Valid @RequestBody OverridePlanRequest request) {
        billingService.overridePlan(tenantId, request.getPlanKey());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/tenants/{tenantId}/aktif")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setTenantAktif(
            @PathVariable Long tenantId,
            @RequestBody Map<String, Boolean> body) {
        billingService.setTenantAktif(tenantId, body.get("aktif"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/tenants/{tenantId}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restoreTenant(@PathVariable Long tenantId) {
        accountDeletionService.restoreAccount(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/tenants/{tenantId}/business-type")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setTenantBusinessType(
            @PathVariable Long tenantId,
            @RequestBody Map<String, String> body) {
        String code = body.get("businessType");
        com.appointflow.tenant.BusinessType type;
        try {
            type = com.appointflow.tenant.BusinessType.valueOf(code);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Geçersiz businessType: " + code, "BAD_REQUEST"));
        }
        billingService.setTenantBusinessType(tenantId, type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
