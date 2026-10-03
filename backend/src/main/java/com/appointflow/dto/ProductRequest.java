package com.appointflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank
    private String ad;

    private String aciklama;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal fiyat;

    @Min(0)
    private Integer stok;

    private String kategori;

    private Boolean aiOneriAktif;

    private List<Long> hizmetIds;
}
