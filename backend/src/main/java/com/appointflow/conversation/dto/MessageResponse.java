package com.appointflow.conversation.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String mesajTipi;
    private String icerik;
    private LocalDateTime olusturmaTarihi;
}
