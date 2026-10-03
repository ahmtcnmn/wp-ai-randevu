package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReminderStatsResponse {
    private long toplam;
    private long beklemede;       // PENDING
    private long gonderildi;      // SENT
    private long yanitlandiEvet;  // RESPONDED_YES
    private long yanitlandiHayir; // RESPONDED_NO
    private long ertelendi;       // SNOOZED
    private long iptal;           // CANCELLED
}
