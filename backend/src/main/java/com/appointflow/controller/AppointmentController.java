package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.AppointmentService;
import com.appointflow.service.ProductSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ProductSaleService productSaleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AppointmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.update(id, request)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            throw com.appointflow.common.ApiException.badRequest("Istek govdesi bos olamaz.");
        }
        String toplamFiyatStr = body.get("toplamFiyat");
        java.math.BigDecimal toplamFiyat = null;
        if (toplamFiyatStr != null && !toplamFiyatStr.isBlank()) {
            try {
                toplamFiyat = new java.math.BigDecimal(toplamFiyatStr);
            } catch (NumberFormatException e) {
                throw com.appointflow.common.ApiException.badRequest("Toplam fiyat sayisal olmali.");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(appointmentService.updateStatus(id, body.get("durum"), toplamFiyat)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String neden = body != null ? body.get("neden") : null;
        return ResponseEntity.ok(ApiResponse.success(appointmentService.cancel(id, neden)));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Void>> addNote(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentNoteRequest request,
            Authentication authentication) {
        appointmentService.addNote(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/products")
    public ResponseEntity<ApiResponse<ProductSaleResponse>> addProductSale(
            @PathVariable Long id, @Valid @RequestBody ProductSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productSaleService.addSaleToAppointment(id, request)));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<java.util.List<ProductSaleResponse>>> getProductSales(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productSaleService.getByRandevu(id)));
    }

    @DeleteMapping("/{id}/products/{saleId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProductSale(@PathVariable Long id, @PathVariable Long saleId) {
        productSaleService.deleteSale(saleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<LocalTime>>> getAvailability(
            @RequestParam Long uzmanId,
            @RequestParam List<Long> hizmetIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAvailability(uzmanId, hizmetIds, tarih)));
    }
}
