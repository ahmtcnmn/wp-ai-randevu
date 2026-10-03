package com.appointflow.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequestSubmission {

    @NotBlank
    @Size(min = 1, max = 100)
    private String ad;

    @NotBlank
    @Size(min = 1, max = 100)
    private String soyad;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9+\\s\\-()]{7,20}$", message = "Telefon yalnizca rakam, +, boslukl ve -() icerebilir, 7-20 karakter.")
    private String telefon;

    @NotBlank
    @Size(min = 10, max = 5000)
    private String mesaj;
}
