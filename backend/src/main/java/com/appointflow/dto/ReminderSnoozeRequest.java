package com.appointflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReminderSnoozeRequest {

    @NotNull
    private LocalDate yeniTarih;
}
