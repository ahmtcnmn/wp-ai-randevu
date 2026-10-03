package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class BranchResponse {
    private Long id;
    private String ad;
    private String adres;
    private String telefon;
    private String email;
    private String whatsappNumarasi;
    private String aciklama;
    private Boolean aktif;
    private LocalDateTime createdAt;
}
