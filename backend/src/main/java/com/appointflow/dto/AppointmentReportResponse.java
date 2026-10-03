package com.appointflow.dto;

import java.util.Map;

public record AppointmentReportResponse(
        Long total,
        Map<String, Long> byStatus,
        Map<Long, Double> revenueByStaff,
        String from,
        String to
) {}
