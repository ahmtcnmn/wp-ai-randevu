package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.GeriBildirimRequest;
import com.appointflow.dto.GeriBildirimResponse;
import com.appointflow.service.GeriBildirimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class GeriBildirimController {

    private final GeriBildirimService geriBildirimService;

    @PostMapping
    public ResponseEntity<ApiResponse<GeriBildirimResponse>> ekle(
            @Valid @RequestBody GeriBildirimRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                geriBildirimService.geriBildirimEkle(request, authentication.getName())));
    }

    @GetMapping("/uzman/{uzmanId}")
    public ResponseEntity<ApiResponse<List<GeriBildirimResponse>>> uzmanYorumlari(@PathVariable Long uzmanId) {
        return ResponseEntity.ok(ApiResponse.success(geriBildirimService.uzmaninYorumlariniGetir(uzmanId)));
    }

    @GetMapping("/sikayetler")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<GeriBildirimResponse>>> sikayetler() {
        return ResponseEntity.ok(ApiResponse.success(geriBildirimService.sadeceSikayetleriGetir()));
    }
}
