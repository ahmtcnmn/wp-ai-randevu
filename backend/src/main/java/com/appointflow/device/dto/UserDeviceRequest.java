package com.appointflow.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDeviceRequest {
    @NotBlank
    private String expoPushToken;

    // ios | android | web — opsiyonel
    private String platform;
}
