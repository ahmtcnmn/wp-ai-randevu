package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.ReminderTemplateRequest;
import com.appointflow.dto.ReminderTemplateResponse;
import com.appointflow.service.ReminderTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminder-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class ReminderTemplateController {

    private final ReminderTemplateService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReminderTemplateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReminderTemplateResponse>> create(@Valid @RequestBody ReminderTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReminderTemplateResponse>> update(@PathVariable Long id, @Valid @RequestBody ReminderTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
