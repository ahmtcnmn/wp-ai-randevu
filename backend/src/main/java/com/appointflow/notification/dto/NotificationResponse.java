package com.appointflow.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String tip;
    private String baslik;
    private String icerik;
    private String link;
    private Boolean okundu;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
