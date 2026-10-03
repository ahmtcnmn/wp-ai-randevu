package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchRequest {
    @NotBlank
    private String ad;
    @NotBlank
    private String adres;
    private String telefon;
    private String email;
    private String whatsappNumarasi;
    private String aciklama;
}
