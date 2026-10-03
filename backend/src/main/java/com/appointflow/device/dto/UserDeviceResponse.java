package com.appointflow.device.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDeviceResponse {
    private Long id;
    private String expoPushToken;
    private String platform;
    private LocalDateTime sonKullanim;
    private LocalDateTime createdAt;
}
