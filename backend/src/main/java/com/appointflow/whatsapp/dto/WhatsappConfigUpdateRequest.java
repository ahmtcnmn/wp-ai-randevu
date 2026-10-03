package com.appointflow.whatsapp.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappConfigUpdateRequest {

    @Size(max = 50, message = "Phone number ID en fazla 50 karakter olabilir")
    private String phoneNumberId;

    @Size(max = 50)
    private String wabaId;

    private String accessToken;

    @Size(max = 100)
    private String verifyToken;

    @Size(max = 200)
    private String appSecret;

    @Size(max = 500)
    private String webhookUrl;

    @Size(max = 20)
    private String displayPhone;

    private Boolean aktif;
}
