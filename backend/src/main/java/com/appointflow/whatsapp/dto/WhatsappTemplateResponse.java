package com.appointflow.whatsapp.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappTemplateResponse {
    private Long id;
    private String ad;
    private String templateKey;
    private String kategori;
    private String dil;
    private String govde;
    private String baslik;
    private String footer;
    private String status;          // PENDING/APPROVED/REJECTED/DRAFT
    private String metaTemplateId;
    private String redSebebi;
}
