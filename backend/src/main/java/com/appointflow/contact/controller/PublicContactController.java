package com.appointflow.contact.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.contact.dto.ContactRequestSubmission;
import com.appointflow.contact.service.ContactRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public iletisim formu — Landing /iletisim sayfasi tarafindan cagrilir.
 * permitAll — SecurityConfig'te /api/v1/public/** zaten acik.
 */
@RestController
@RequestMapping("/api/v1/public/contact")
@RequiredArgsConstructor
public class PublicContactController {

    private final ContactRequestService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @Valid @RequestBody ContactRequestSubmission body,
            HttpServletRequest req) {
        service.submit(body, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
