package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.ServiceRequest;
import com.appointflow.dto.ServiceResponse;
import com.appointflow.service.HizmetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final HizmetService hizmetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(hizmetService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hizmetService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> create(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hizmetService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServiceResponse>> update(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hizmetService.update(id, request)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        hizmetService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        hizmetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
