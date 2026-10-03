package com.appointflow.dto;

import com.appointflow.tenant.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GirisRequest {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String sifre;

    /**
     * Mobil app build-time olarak set eder (örn. Berber app → BARBER).
     * Backend tenant.businessType ile karşılaştırır — uyuşmazsa WRONG_APP_FOR_BUSINESS_TYPE.
     * Web ve eski mobil sürümler bunu göndermez → kontrol atlanır (backward compatible).
     */
    private BusinessType expectedBusinessType;
}
