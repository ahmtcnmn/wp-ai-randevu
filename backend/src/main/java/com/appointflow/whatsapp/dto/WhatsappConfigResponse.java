package com.appointflow.whatsapp.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappConfigResponse {
    private Long id;
    private String phoneNumberId;
    private String wabaId;
    private String displayPhone;
    private String webhookUrl;
    private Boolean aktif;
    private boolean tokenConfigured;
    private boolean appSecretConfigured;
}
