package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentReminderResponse {
    private Long id;
    private Long randevuId;
    private Long customerId;
    private String musteriAd;
    private String musteriTelefon;
    private Long templateId;
    private String mesaj;
    private String kanal;
    private LocalDate gonderimTarihi;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
    private LocalDate snoozedUntil;
    private Integer snoozeCount;
    private LocalDateTime createdAt;
}
