package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorDisableRequest {
    @NotBlank
    private String sifre;
}
