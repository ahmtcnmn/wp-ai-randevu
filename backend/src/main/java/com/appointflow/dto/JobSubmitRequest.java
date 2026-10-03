package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobSubmitRequest {
    @NotBlank
    private String jobType;
    private String payload;
    @NotBlank
    private String idempotencyKey;
}
