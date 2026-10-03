package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.ProductRequest;
import com.appointflow.dto.ProductResponse;
import com.appointflow.dto.ProductSaleResponse;
import com.appointflow.dto.ProductSalesSummaryResponse;
import com.appointflow.dto.StandaloneProductSaleRequest;
import com.appointflow.service.ProductSaleService;
import com.appointflow.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSaleService productSaleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Belirli bir hizmet için önerilen ürünler (aiOneriAktif=true ve aktif=true).
     * Randevu detay sayfası "Önerilen ürünler" kartında kullanılır.
     */
    @GetMapping("/recommended/hizmet/{hizmetId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRecommendedForHizmet(@PathVariable Long hizmetId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRecommendedForHizmet(hizmetId)));
    }

    /**
     * Randevu dışı (standalone) ürün satışı. Dashboard hızlı satış butonu kullanır.
     * customerId ve staffId opsiyonel — staffId verilirse komisyon işler.
     */
    @PostMapping("/sales/standalone")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductSaleResponse>> createStandaloneSale(
            @Valid @RequestBody StandaloneProductSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productSaleService.createStandalone(
                        request.getProductId(),
                        request.getAdet(),
                        request.getCustomerId(),
                        request.getStaffId())));
    }

    @GetMapping("/sales/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductSalesSummaryResponse>> getSalesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(productSaleService.getSummary(dateFrom, dateTo)));
    }

    @GetMapping("/{id}/sales")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProductSaleResponse>>> getProductHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productSaleService.getProductHistory(id)));
    }
}
