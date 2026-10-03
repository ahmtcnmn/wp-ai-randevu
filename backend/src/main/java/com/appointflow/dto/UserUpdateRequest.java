package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String soyad;
    @NotBlank
    private String telefon;
    private String rol;
    private Long subeId;
    private String pozisyon;
    /** Bu çalışanın yapabileceği hizmetlerin id listesi. Null gönderilirse mevcut atamalar değişmez. Boş liste gönderilirse tüm atamalar silinir. */
    private java.util.List<Long> hizmetIds;
}
