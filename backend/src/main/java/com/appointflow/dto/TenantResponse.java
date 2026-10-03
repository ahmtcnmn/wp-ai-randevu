package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TenantResponse {
    private Long id;
    private String ad;
    private String slug;
    private String email;
    private String telefon;
    private String adres;
    private String sehir;
    private String ulke;
    private String tckn;
    private String logoUrl;
    private Boolean aktif;
    private Boolean onboardingCompleted;
}
