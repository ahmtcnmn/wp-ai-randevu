package com.appointflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String soyad;
    @Email @NotBlank
    private String email;
    @NotBlank
    @Size(min = 6, message = "Sifre en az 6 karakter olmalidir")
    private String sifre;
    @NotBlank
    private String telefon;
    @NotNull
    private String rol; // STAFF, BRANCH_MANAGER, ADMIN
    private Long subeId;

    /** Sektör-bazlı pozisyon — opsiyonel. StaffPosition enum'undan biri. */
    private String pozisyon;

    /** Bu çalışanın yapabileceği hizmetlerin id listesi. */
    private java.util.List<Long> hizmetIds;
}
