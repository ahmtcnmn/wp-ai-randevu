package com.appointflow.ai.controller;

import com.appointflow.ai.dto.AiConfigResponse;
import com.appointflow.ai.dto.AiConfigUpdateRequest;
import com.appointflow.ai.service.AiConfigService;
import com.appointflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AiConfigResponse>> getConfig() {
        AiConfigResponse config = aiConfigService.getConfig();
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AiConfigResponse>> updateConfig(
            @Valid @RequestBody AiConfigUpdateRequest request) {
        AiConfigResponse config = aiConfigService.updateConfig(request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }
}
