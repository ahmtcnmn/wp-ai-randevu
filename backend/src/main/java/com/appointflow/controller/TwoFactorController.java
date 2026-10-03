package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.TwoFactorDisableRequest;
import com.appointflow.dto.TwoFactorEnableResponse;
import com.appointflow.dto.TwoFactorSetupResponse;
import com.appointflow.dto.TwoFactorVerifyRequest;
import com.appointflow.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(twoFactorService.beginSetup(auth.getName())));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TwoFactorEnableResponse>> verify(
            Authentication auth, @Valid @RequestBody TwoFactorVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                twoFactorService.verifyAndEnable(auth.getName(), request.getCode())));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable(
            Authentication auth, @Valid @RequestBody TwoFactorDisableRequest request) {
        twoFactorService.disable(auth.getName(), request.getSifre());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/recovery-codes/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> recoveryCodeCount(Authentication auth) {
        long count = twoFactorService.countUnusedRecoveryCodesByEmail(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unusedCount", count)));
    }

    @PostMapping("/recovery-codes/regenerate")
    public ResponseEntity<ApiResponse<List<String>>> regenerateRecoveryCodes(Authentication auth) {
        List<String> codes = twoFactorService.regenerateRecoveryCodes(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(codes));
    }
}
