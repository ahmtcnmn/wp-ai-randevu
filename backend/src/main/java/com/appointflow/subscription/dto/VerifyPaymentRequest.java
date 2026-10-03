package com.appointflow.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String planKey;
}
