package com.appointflow.conversation.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private Long id;
    private Long customerId;
    private String customerPhone;
    private String customerName;
    private String durum;
    private Long assignedUserId;
    private String kanal;
    private LocalDateTime sonMesajZamani;
    private LocalDateTime olusturmaTarihi;
    private LocalDateTime kapatmaTarihi;
}
