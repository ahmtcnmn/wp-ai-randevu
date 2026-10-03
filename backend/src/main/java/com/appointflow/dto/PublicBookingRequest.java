package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PublicBookingRequest {
    @NotBlank
    private String musteriAd;
    @NotBlank
    private String musteriSoyad;
    @NotBlank
    private String musteriTelefon;

    @NotNull
    private Long uzmanId;
    @NotNull
    private List<Long> hizmetIds;
    @NotNull
    private LocalDateTime tarihSaat;
    private String not;
}
