package com.appointflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StandaloneProductSaleRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer adet;

    /** Opsiyonel — müşteri seçimi (sadakat puanı, satış geçmişi için). */
    private Long customerId;

    /** Opsiyonel — komisyon hesabı için. */
    private Long staffId;
}
