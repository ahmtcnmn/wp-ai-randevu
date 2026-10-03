package com.appointflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppointmentUpdateRequest {

    @NotEmpty
    private List<Long> hizmetIds;

    @NotNull
    private LocalDateTime tarihSaat;
}
