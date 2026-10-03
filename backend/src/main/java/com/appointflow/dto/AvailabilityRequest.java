package com.appointflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AvailabilityRequest {
    @NotNull
    private Long uzmanId;
    @NotNull
    private List<Long> hizmetIds;
    @NotNull
    private LocalDate tarih;
}
