package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorLoginRequest {
    @NotBlank
    private String tempToken;

    @NotBlank
    private String code;
}
