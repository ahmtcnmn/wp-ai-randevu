package com.appointflow.controller;

import com.appointflow.common.ApiResponse;
import com.appointflow.dto.*;
import com.appointflow.service.AccountDeletionService;
import com.appointflow.service.AuthService;
import com.appointflow.service.EmailVerificationService;
import com.appointflow.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final AccountDeletionService accountDeletionService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody KayitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.kayitOl(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody GirisRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.girisYap(request)));
    }

    @PostMapping("/login-2fa")
    public ResponseEntity<ApiResponse<AuthResponse>> loginTwoFactor(
            @Valid @RequestBody TwoFactorLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.verifyTwoFactorLogin(request.getTempToken(), request.getCode())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.updateProfile(authentication.getName(), request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        // Email enumeration onlemek icin her zaman 200 doneriz.
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(Authentication authentication) {
        emailVerificationService.resendVerification(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Hesap silme — soft delete. 30 gün geri alma penceresi var.
     * Sadece OWNER yapabilir.
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication authentication) {
        accountDeletionService.requestAccountDeletion(authentication.getName());
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
