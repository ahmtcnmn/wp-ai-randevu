package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String ad;
    private String soyad;
    private String rol;
    private Long tenantId;

    /** 2FA aktifse login'de bu alanlar dolar, token/refreshToken null gelir.
     *  Frontend tempToken'i kullanarak /auth/login-2fa'a code ile gider. */
    private Boolean requires2fa;
    private String tempToken;

    // Eski constructor — geriye uyumluluk
    public AuthResponse(String token, String email, String ad, String soyad, String rol) {
        this.token = token;
        this.email = email;
        this.ad = ad;
        this.soyad = soyad;
        this.rol = rol;
    }
}
