package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.RaporDTO;
import com.appointflow.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin-rapor")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/genel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RaporDTO>> genelRapor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bitis) {
        return ResponseEntity.ok(ApiResponse.success(adminService.genelRaporGetir(baslangic, bitis)));
    }

    @GetMapping("/uzman/{uzmanId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RaporDTO>> uzmanRapor(
            @PathVariable Long uzmanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baslangic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bitis) {
        return ResponseEntity.ok(ApiResponse.success(adminService.uzmanRaporGetir(uzmanId, baslangic, bitis)));
    }
}
