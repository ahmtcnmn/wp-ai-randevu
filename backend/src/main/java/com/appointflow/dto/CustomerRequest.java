package com.appointflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
    private String ad;
    @NotBlank
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
    private String soyad;
    @NotBlank
    @Pattern(regexp = "^[0-9+\\s\\-()]{7,20}$", message = "Telefon yalnizca rakam, +, boslukl ve -() icerebilir, 7-20 karakter.")
    private String telefon;
    @Email
    @Size(max = 200)
    private String email;
    @Size(max = 1000)
    private String notlar;
}
