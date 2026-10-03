package com.appointflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppointmentRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private Long uzmanId;
    @NotEmpty(message = "En az 1 hizmet secilmeli.")
    private List<Long> hizmetIds;
    @NotNull
    private LocalDateTime tarihSaat;
    @Size(max = 1000, message = "Not en fazla 1000 karakter olabilir.")
    private String not;
    @Size(max = 32)
    private String kaynak; // MANUAL, WEB, WHATSAPP
}
