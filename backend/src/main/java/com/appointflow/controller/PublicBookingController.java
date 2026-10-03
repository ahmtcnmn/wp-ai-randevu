package com.appointflow.controller;

import com.appointflow.common.ApiException;
import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.repository.TenantRepository;
import com.appointflow.service.AppointmentService;
import com.appointflow.service.BranchService;
import com.appointflow.service.CustomerService;
import com.appointflow.service.HizmetService;
import com.appointflow.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Kimlik doğrulaması gerektirmeyen public randevu alma endpoint'leri.
 * Tüm istekler ?tenantId= parametresi ile hangi işletmeye ait olduğunu belirtmek zorundadır.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BranchService branchService;
    private final HizmetService hizmetService;
    private final CustomerService customerService;
    private final AppointmentService appointmentService;
    private final TenantRepository tenantRepository;

    private void validateTenant(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw ApiException.notFound("Isletme bulunamadi: " + tenantId);
        }
    }

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranches(@RequestParam Long tenantId) {
        validateTenant(tenantId);
        TenantContext.set(tenantId);
        try {
            return ResponseEntity.ok(ApiResponse.success(branchService.getAll()));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServices(@RequestParam Long tenantId) {
        validateTenant(tenantId);
        TenantContext.set(tenantId);
        try {
            return ResponseEntity.ok(ApiResponse.success(hizmetService.getAll()));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStaff(@RequestParam Long tenantId) {
        validateTenant(tenantId);
        TenantContext.set(tenantId);
        try {
            return ResponseEntity.ok(ApiResponse.success(appointmentService.getStaffByTenant(tenantId)));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<LocalTime>>> getAvailability(
            @RequestParam Long tenantId,
            @RequestParam Long uzmanId,
            @RequestParam List<Long> hizmetIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih) {
        validateTenant(tenantId);
        TenantContext.set(tenantId);
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    appointmentService.getAvailability(uzmanId, hizmetIds, tarih)));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @RequestParam Long tenantId,
            @Valid @RequestBody PublicBookingRequest request) {
        validateTenant(tenantId);
        TenantContext.set(tenantId);
        try {
            Long customerId = getOrCreateCustomer(tenantId, request);

            AppointmentRequest appointmentRequest = new AppointmentRequest();
            appointmentRequest.setCustomerId(customerId);
            appointmentRequest.setUzmanId(request.getUzmanId());
            appointmentRequest.setHizmetIds(request.getHizmetIds());
            appointmentRequest.setTarihSaat(request.getTarihSaat());
            appointmentRequest.setNot(request.getNot());
            appointmentRequest.setKaynak("WEB");

            return ResponseEntity.ok(ApiResponse.success(appointmentService.create(appointmentRequest)));
        } finally {
            TenantContext.clear();
        }
    }

    private Long getOrCreateCustomer(Long tenantId, PublicBookingRequest request) {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setAd(request.getMusteriAd());
        customerRequest.setSoyad(request.getMusteriSoyad());
        customerRequest.setTelefon(request.getMusteriTelefon());

        try {
            CustomerResponse response = customerService.create(customerRequest);
            return response.getId();
        } catch (ApiException ex) {
            // Telefon numarası zaten kayıtlıysa mevcut müşteriyi bul
            if ("CONFLICT".equals(ex.getErrorCode())) {
                return customerService.findByTelefon(tenantId, request.getMusteriTelefon());
            }
            throw ex;
        }
    }
}
