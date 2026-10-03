package com.appointflow.subscription.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.subscription.dto.*;
import com.appointflow.subscription.service.BillingService;
import com.appointflow.subscription.service.QuotaService;
import com.appointflow.subscription.service.SubscriptionPlanService;
import com.appointflow.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final QuotaService quotaService;
    private final SubscriptionPlanService planService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlans()));
    }

    @GetMapping("/subscription")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(billingService.getSubscription(tenantId)));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(billingService.getInvoices(tenantId)));
    }

    @GetMapping("/quota")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUsageResponse>> getQuota() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(quotaService.getUsage(tenantId)));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CheckoutInitResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                billingService.initializeCheckout(tenantId, request.getPlanKey())));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> verify(
            @Valid @RequestBody VerifyPaymentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                billingService.verifyAndActivate(tenantId, request.getToken(), request.getPlanKey())));
    }

    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<CheckoutInitResponse>> upgrade(
            @Valid @RequestBody CheckoutRequest request) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                billingService.upgradeSubscription(tenantId, request.getPlanKey())));
    }

    @DeleteMapping("/subscription")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(billingService.cancelSubscription(tenantId)));
    }

    /**
     * İyzico POST callback — permitAll, no JWT required.
     * İyzico posts token + conversationId as form params, we verify and redirect to frontend.
     */
    @PostMapping(value = "/iyzico-callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> iyzicoCallback(HttpServletRequest request) {
        // Tüm parametreleri logla — İyzico'nun gönderdiği param adlarını görmek için
        request.getParameterMap().forEach((key, values) ->
                log.info("İyzico callback param: {}={}", key, String.join(",", values)));

        String token = request.getParameter("token");
        String convId = request.getParameter("conversationId");
        if (convId == null) convId = request.getParameter("conversation_id");

        String redirectUrl = billingService.handleIyzicoCallback(token, convId);

        // 302 redirect yerine HTML meta-refresh döndür.
        // Tarayıcı banka sayfasından gelen cross-origin POST'ta 302'yi dosya olarak indirebilir.
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta http-equiv="refresh" content="0;url=%s">
                  <title>Yönlendiriliyor...</title>
                </head>
                <body>
                  <script>window.location.href = '%s';</script>
                  <p>Yönlendiriliyor, lütfen bekleyin...</p>
                </body>
                </html>
                """.formatted(redirectUrl, redirectUrl);

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
