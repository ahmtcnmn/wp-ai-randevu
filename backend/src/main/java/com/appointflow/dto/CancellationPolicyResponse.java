package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancellationPolicyResponse {
    private Integer saatOnce;
    private String mesaj;
}
