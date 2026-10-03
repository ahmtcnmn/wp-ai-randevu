package com.appointflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GeriBildirimRequest {
    @NotNull
    private Long randevuId;

    @Min(1) @Max(5)
    @NotNull
    private Integer puan;

    private String yorum;

    // Opsiyonel, frontend'den "Bu bir şikayettir" butonu seçilirse.
    // Seçilmezse ve puan 1-2 ise backend bunu otomatik olarak şikayet kabul eder.
    private Boolean sikayetOlarakIsaretle;
}
