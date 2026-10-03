package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentReminderRequest {

    @NotNull
    private LocalDate gonderimTarihi;

    @NotBlank
    private String mesaj;

    // Opsiyonel — şablondan türetiliyorsa
    private Long templateId;

    // Opsiyonel — default WHATSAPP. Izinli: WHATSAPP, SMS, EMAIL
    private String kanal;
}
