package com.appointflow.dto;

public record DashboardReportResponse(
        Long totalAppointmentsThisMonth,
        Double revenueThisMonth,
        Long newCustomersThisMonth,
        Long totalCustomers,
        Long activeCampaigns,
        Double avgAppointmentValue,
        String generatedAt
) {}
