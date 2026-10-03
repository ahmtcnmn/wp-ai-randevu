package com.appointflow.whatsapp.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.whatsapp.dto.WhatsappTemplateRequest;
import com.appointflow.whatsapp.dto.WhatsappTemplateResponse;
import com.appointflow.whatsapp.service.WhatsappTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/whatsapp/templates")
@RequiredArgsConstructor
public class WhatsappTemplateController {

    private final WhatsappTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<WhatsappTemplateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappTemplateResponse>> create(
            @Valid @RequestBody WhatsappTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(templateService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<WhatsappTemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody WhatsappTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(templateService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<WhatsappTemplateResponse>> submitToMeta(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.submitToMeta(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
