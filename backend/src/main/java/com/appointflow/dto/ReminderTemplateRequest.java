package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReminderTemplateRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String mesaj;
    @NotNull
    private Integer gunSonra;
    /** true = randevudan ONCE gonder; false (default) = SONRA gonder. */
    private Boolean oncesi;
    /** Birim: GUN veya SAAT. Default GUN. */
    private String birim;
    private Long hizmetId;
    private Boolean aktif;
    // Opsiyonel — default WHATSAPP. Izinli: WHATSAPP, SMS, EMAIL
    private String kanal;
}
