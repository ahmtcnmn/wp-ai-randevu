package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/commission")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    // ─── Commission Rules ──────────────────────────────────────────────────────

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CommissionRuleResponse>>> getRules(
            @RequestParam(required = false) String scope) {
        if (scope != null && !scope.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(commissionService.getRulesByScope(scope)));
        }
        return ResponseEntity.ok(ApiResponse.success(commissionService.getRules()));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> createRule(
            @Valid @RequestBody CommissionRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.createRule(request)));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CommissionRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.updateRule(id, request)));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        commissionService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ─── Earning Periods ───────────────────────────────────────────────────────

    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EarningPeriodResponse>>> getPeriods() {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getPeriods()));
    }

    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EarningPeriodResponse>> createPeriod(
            @Valid @RequestBody EarningPeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.createPeriod(request)));
    }

    @PostMapping("/periods/{id}/collect")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EarningPeriodResponse>> collect(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.collect(id)));
    }

    @GetMapping("/periods/{id}/earnings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EarningResponse>>> getPeriodEarnings(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getPeriodEarnings(id)));
    }

    // ─── Staff Self-Service ────────────────────────────────────────────────────

    @GetMapping("/my-earnings")
    public ResponseEntity<ApiResponse<List<EarningResponse>>> getMyEarnings(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getMyEarnings(authentication.getName())));
    }

    @GetMapping("/my-summary")
    public ResponseEntity<ApiResponse<EarningsSummaryResponse>> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getMySummary(authentication.getName())));
    }

    // ─── Admin View ────────────────────────────────────────────────────────────

    @GetMapping("/staff/{staffId}/earnings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EarningResponse>>> getStaffEarnings(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getStaffEarnings(staffId)));
    }

    @GetMapping("/staff/{staffId}/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EarningsSummaryResponse>> getStaffSummary(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(commissionService.getStaffSummary(staffId)));
    }
}
