package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.FeatureResponse;
import com.appointflow.dto.FeatureToggleRequest;
import com.appointflow.service.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FeatureResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(featureService.getAllFeatures()));
    }

    @PutMapping("/{featureKey}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<FeatureResponse>> toggle(
            @PathVariable String featureKey,
            @Valid @RequestBody FeatureToggleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(featureService.toggleFeature(featureKey, request.getEnabled())));
    }
}
