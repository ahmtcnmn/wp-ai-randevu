package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // Tüm hatırlatmaları listele (OWNER/ADMIN/BRANCH_MANAGER)
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<AppointmentReminderResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(reminderService.getAll()));
    }

    // Bekleyen hatırlatmalar
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<AppointmentReminderResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(reminderService.getPending()));
    }

    // Randevuya ait hatırlatmalar
    @GetMapping("/randevu/{randevuId}")
    public ResponseEntity<ApiResponse<List<AppointmentReminderResponse>>> getByRandevu(
            @PathVariable Long randevuId) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.getByRandevu(randevuId)));
    }

    // Randevuya hatırlatma ekle (TAMAMLANDI sonrası çalışan ekler)
    @PostMapping("/randevu/{randevuId}")
    public ResponseEntity<ApiResponse<AppointmentReminderResponse>> create(
            @PathVariable Long randevuId,
            @Valid @RequestBody AppointmentReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.createReminder(randevuId, request)));
    }

    // Ertele
    @PostMapping("/{id}/snooze")
    public ResponseEntity<ApiResponse<AppointmentReminderResponse>> snooze(
            @PathVariable Long id,
            @Valid @RequestBody ReminderSnoozeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.snooze(id, request)));
    }

    // İptal et
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentReminderResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.cancel(id)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ReminderStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(reminderService.getStats()));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<AppointmentReminderResponse>> resend(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.resend(id)));
    }
}
