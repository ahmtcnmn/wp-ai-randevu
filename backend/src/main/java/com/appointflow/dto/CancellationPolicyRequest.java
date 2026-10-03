package com.appointflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CancellationPolicyRequest {

    @Min(0) @Max(168)  // 0-168 saat (1 hafta)
    private Integer saatOnce;

    private String mesaj;
}
