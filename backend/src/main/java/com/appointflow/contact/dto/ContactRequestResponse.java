package com.appointflow.contact.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContactRequestResponse {
    private Long id;
    private String ad;
    private String soyad;
    private String email;
    private String telefon;
    private String mesaj;
    private String ipAddress;
    private String userAgent;
    private String durum;
    private String superAdminNotu;
    private Long cevaplayanUserId;
    private LocalDateTime cevaplanmaTarihi;
    private LocalDateTime createdAt;
}
