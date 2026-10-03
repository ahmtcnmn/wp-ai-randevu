package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.CustomerImportService;
import com.appointflow.service.CustomerService;
import com.appointflow.service.SegmentationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final SegmentationService segmentationService;
    private final CustomerImportService customerImportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> block(@PathVariable Long id) {
        customerService.block(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> unblock(@PathVariable Long id) {
        customerService.unblock(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<CustomerResponse>> addTag(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(customerService.addTag(id, body.get("etiket"))));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        customerService.removeTag(id, tagId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ─── Segmentation ──────────────────────────────────────────────────────────

    @GetMapping("/segments/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<SegmentSummaryResponse>> getSegmentSummary() {
        return ResponseEntity.ok(ApiResponse.success(segmentationService.getSummary()));
    }

    @GetMapping("/segments/{segmentType}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerSegmentResponse>>> getBySegment(
            @PathVariable String segmentType) {
        return ResponseEntity.ok(ApiResponse.success(segmentationService.getBySegmentType(segmentType)));
    }

    @GetMapping("/{id}/segment")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> getCustomerSegment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(segmentationService.getCustomerSegment(id)));
    }

    @PostMapping("/{id}/segment/recalculate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> recalculate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(segmentationService.recalculateForCustomer(id)));
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerImportResponse>> importCsv(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(customerImportService.importCsv(file)));
    }
}
