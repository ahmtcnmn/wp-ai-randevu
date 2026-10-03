package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String soyad;
    @NotBlank
    private String telefon;
}
