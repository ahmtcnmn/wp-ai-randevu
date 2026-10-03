package com.appointflow.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfigUpdateRequest {

    @Size(max = 100, message = "Persona adı en fazla 100 karakter olabilir")
    private String personaAdi;

    private String sistemPromptu;

    /** Onboarding'de alınan işletme tanıtım metni — AI prompt'una iletilir. */
    private String isletmeAciklamasi;

    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    private String dil;

    private Boolean fiyatBilgisiGoster;
    private Boolean otomatikOnay;
    private String handoffKelimeleri;
    private Integer maxToken;

    @Size(max = 50, message = "Model adı en fazla 50 karakter olabilir")
    private String model;

    private Boolean aktif;
}
