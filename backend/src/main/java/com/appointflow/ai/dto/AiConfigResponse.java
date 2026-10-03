package com.appointflow.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfigResponse {
    private Long id;
    private String personaAdi;
    private String sistemPromptu;
    private String isletmeAciklamasi;
    private String dil;
    private Boolean fiyatBilgisiGoster;
    private Boolean otomatikOnay;
    private String handoffKelimeleri;
    private Integer maxToken;
    private String model;
    private Boolean aktif;
}
