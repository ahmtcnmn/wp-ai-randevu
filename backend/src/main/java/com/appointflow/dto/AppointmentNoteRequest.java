package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppointmentNoteRequest {
    @NotBlank
    private String icerik;
    private String tur; // INTERNAL, MUSTERI, REMINDER (default: INTERNAL)
}
