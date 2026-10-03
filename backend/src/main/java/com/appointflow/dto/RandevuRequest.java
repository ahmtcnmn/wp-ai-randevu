package com.appointflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RandevuRequest {
    @NotNull
    private Long uzmanId;
    @NotNull
    private Long hizmetId;
    @NotNull
    private LocalDateTime tarihSaat;
    private String not;
}
