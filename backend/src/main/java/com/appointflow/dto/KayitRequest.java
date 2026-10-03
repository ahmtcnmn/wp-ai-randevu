package com.appointflow.dto;

import com.appointflow.tenant.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KayitRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String soyad;
    @Email @NotBlank
    private String email;
    @NotBlank
    @Size(min = 8, max = 100, message = "Sifre en az 8 karakter olmalidir")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
            message = "Sifre buyuk harf, kucuk harf ve rakam icermelidir"
    )
    private String sifre;
    @NotBlank
    @Pattern(regexp = "^[0-9+]{10,15}$", message = "Telefon 10-15 hane, sadece rakam ve + olabilir")
    private String telefon;

    /**
     * İşletmenin sektörü. Opsiyonel — gönderilmezse OTHER atanır.
     * Mobil app build-time'da bu alanı set eder, web register'da kullanıcı seçer.
     */
    private BusinessType businessType;

    /** Opsiyonel işletme adı — kayıt sırasında alınırsa tenant.ad olarak set edilir. */
    private String isletmeAdi;
}
