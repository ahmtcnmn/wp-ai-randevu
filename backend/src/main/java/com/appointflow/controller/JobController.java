package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.BackgroundJobResponse;
import com.appointflow.dto.DeadLetterJobResponse;
import com.appointflow.dto.JobSubmitRequest;
import com.appointflow.service.BackgroundJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class JobController {

    private final BackgroundJobService jobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BackgroundJobResponse>>> getJobs() {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobs()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackgroundJobResponse>> submit(@Valid @RequestBody JobSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(jobService.submitFromRequest(request)));
    }

    @GetMapping("/dead-letter")
    public ResponseEntity<ApiResponse<List<DeadLetterJobResponse>>> getDeadLetters() {
        return ResponseEntity.ok(ApiResponse.success(jobService.getDeadLetters()));
    }

    @PostMapping("/dead-letter/{id}/retry")
    public ResponseEntity<ApiResponse<BackgroundJobResponse>> retryDeadLetter(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.retryDeadLetter(id)));
    }

    @PostMapping("/dead-letter/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveDeadLetter(@PathVariable Long id) {
        jobService.resolveDeadLetter(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
