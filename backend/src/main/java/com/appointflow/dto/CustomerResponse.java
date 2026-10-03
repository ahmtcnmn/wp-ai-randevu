package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long id;
    private String ad;
    private String soyad;
    private String telefon;
    private String email;
    private String notlar;
    private Integer gelmemeSayisi;
    private Boolean karaListedeMi;
    private Integer sadakatPuani;
    private LocalDateTime sonZiyaret;
    private List<String> etiketler;
    private LocalDateTime createdAt;
}
