package com.appointflow.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappTemplateRequest {

    @NotBlank(message = "Sablon adi bos olamaz")
    @Size(max = 100)
    private String ad;

    @NotBlank(message = "Template key bos olamaz")
    @Size(max = 50)
    private String templateKey;

    private String kategori; // UTILITY, MARKETING, AUTHENTICATION

    @Size(max = 10)
    private String dil;

    @NotBlank(message = "Govde bos olamaz")
    private String govde;

    @Size(max = 200)
    private String baslik;

    @Size(max = 200)
    private String footer;
}
