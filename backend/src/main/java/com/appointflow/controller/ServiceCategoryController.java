package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.ServiceCategoryRequest;
import com.appointflow.dto.ServiceCategoryResponse;
import com.appointflow.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> create(@Valid @RequestBody ServiceCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> update(@PathVariable Long id, @Valid @RequestBody ServiceCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorder(@RequestBody List<Long> ids) {
        service.reorder(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
