package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String ad;
    private String soyad;
    private String email;
    private String telefon;
    private String rol;
    private Long subeId;
    private String subeAd;
    private String pozisyon;
    private String pozisyonAd;
    /** Bu çalışanın yapabileceği hizmetlerin id listesi (StaffService tablosundan). */
    private java.util.List<Long> hizmetIds;
    private Boolean aktif;
    private Boolean emailDogrulandi;
    private LocalDateTime sonGirisTarihi;
    private LocalDateTime createdAt;
}
