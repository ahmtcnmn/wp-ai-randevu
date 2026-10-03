package com.appointflow.whatsapp.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.whatsapp.dto.WhatsappConfigResponse;
import com.appointflow.whatsapp.dto.WhatsappConfigUpdateRequest;
import com.appointflow.whatsapp.service.WhatsappConfigService;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import com.appointflow.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp/config")
@RequiredArgsConstructor
public class WhatsappConfigController {

    private final WhatsappConfigService configService;
    private final WhatsappMessageService messageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(configService.getConfig()));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<WhatsappConfigResponse>> updateConfig(
            @Valid @RequestBody WhatsappConfigUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(configService.updateConfig(request)));
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendTestMessage(
            @RequestBody Map<String, String> request) {
        String phone = request.get("telefon");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Telefon numarası gerekli"));
        }
        Long tenantId = TenantContext.getTenantId();
        messageService.sendTextMessage(tenantId, phone, "AppointFlow test mesajı - WhatsApp bağlantınız başarılı!");
        return ResponseEntity.ok(ApiResponse.success(Map.of("durum", "Test mesajı gönderildi")));
    }
}
