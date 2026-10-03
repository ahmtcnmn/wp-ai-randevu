package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.UserCreateRequest;
import com.appointflow.dto.UserResponse;
import com.appointflow.dto.UserUpdateRequest;
import com.appointflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.update(id, request)));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.activate(id)));
    }

    /** Doğrulama mailini tekrar gönder (owner çalışan eklerken mail gitmediyse). */
    @PostMapping("/{id}/resend-verification")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@PathVariable Long id) {
        userService.resendVerificationForStaff(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** Çalışanın emailini manuel doğrulanmış işaretle (mail gitmediyse). */
    @PutMapping("/{id}/mark-email-verified")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> markEmailVerified(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.markEmailVerified(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
