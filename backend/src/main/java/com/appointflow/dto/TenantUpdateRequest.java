package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantUpdateRequest {
    @NotBlank
    private String ad;
    private String email;
    private String telefon;
    private String adres;
    @Size(max = 100)
    private String sehir;
    @Size(max = 50)
    private String ulke;
    @Pattern(regexp = "^\\d{11}$|^$", message = "TC Kimlik No 11 hane olmali.")
    private String tckn;
    private String logoUrl;
}
