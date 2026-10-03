package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GeriBildirimResponse {
    private Long id;
    private Long randevuId;
    private Long uzmanId;
    private String uzmanAd;
    private Integer puan;
    private String yorum;
    private Boolean sikayetVarmi;
    private LocalDateTime tarih;
}
