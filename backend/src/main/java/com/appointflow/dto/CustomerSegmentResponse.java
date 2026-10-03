package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerSegmentResponse {
    private Long customerId;
    private String musteriAd;
    private String telefon;
    private String segmentType;
    private Integer totalAppointments;
    private Double totalSpent;
    private Double avgSpentPerVisit;
    private Integer daysSinceLastVisit;
    private Integer noShowCount;
    private LocalDateTime calculatedAt;
}
