package com.appointflow.contact.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.contact.dto.ContactRequestResponse;
import com.appointflow.contact.service.ContactRequestService;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SUPER_ADMIN icin contact request yonetim endpoint'leri.
 */
@RestController
@RequestMapping("/api/v1/admin/contact-requests")
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestService service;
    private final KullaniciRepository kullaniciRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<ContactRequestResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String durum) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(service.list(durum, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContactRequestResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String durum = body.get("durum");
        String not = body.get("not");
        Long userId = auth != null
                ? kullaniciRepository.findByEmail(auth.getName()).map(Kullanici::getId).orElse(null)
                : null;
        return ResponseEntity.ok(ApiResponse.success(
                service.updateStatus(id, durum, not, userId)));
    }
}
