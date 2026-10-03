package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.BranchRequest;
import com.appointflow.dto.BranchResponse;
import com.appointflow.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BranchResponse>> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<BranchResponse>> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.update(id, request)));
    }

    /**
     * Şube pasifleştirme (eskiden delete). Veri kaybı önlemek için silme YOK.
     * Aktif=false yapılır, tarihsel veri sorgularda görünür.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        branchService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        branchService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
