package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ServiceRequest {
    @NotBlank
    private String ad;
    private String aciklama;
    @NotNull @Positive
    private Integer sureDakika;
    @NotNull @Positive
    private Double fiyat;
    private Long kategoriId;
    private Integer bufferOnceDk;
    private Integer bufferSonraDk;
    private String takvimRengi;
    /**
     * Hizmeti verebilen calisanlarin id listesi. Bos veya null gelirse
     * tum aktif calisanlar verebilir kabul edilir (frontend tarafi filtre yapar).
     */
    private List<Long> staffIds;
}
