package com.appointflow.dto;

import java.util.Map;

public record RevenueReportResponse(
        Double totalRevenue,
        Map<Long, Double> byStaff,
        Map<String, Double> byService,
        Double avgRevenuePerAppointment,
        String from,
        String to
) {}
