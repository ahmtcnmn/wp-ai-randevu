package com.appointflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeatureToggleRequest {
    @NotNull
    private Boolean enabled;
}
