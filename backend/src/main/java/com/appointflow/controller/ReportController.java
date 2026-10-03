package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.ReportService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN','BRANCH_MANAGER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardReportResponse> dashboard() {
        return ApiResponse.success(reportService.getDashboard(TenantContext.getTenantId()));
    }

    @GetMapping("/appointments")
    public ApiResponse<AppointmentReportResponse> appointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.success(reportService.getAppointmentReport(TenantContext.getTenantId(), dateFrom, dateTo));
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.success(reportService.getRevenueReport(TenantContext.getTenantId(), dateFrom, dateTo));
    }

    @GetMapping("/customers")
    public ApiResponse<CustomerReportResponse> customers() {
        return ApiResponse.success(reportService.getCustomerReport(TenantContext.getTenantId()));
    }

    @GetMapping("/campaigns")
    public ApiResponse<CampaignReportResponse> campaigns() {
        return ApiResponse.success(reportService.getCampaignReport(TenantContext.getTenantId()));
    }
}
