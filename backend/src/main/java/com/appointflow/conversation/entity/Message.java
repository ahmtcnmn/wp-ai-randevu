package com.appointflow.conversation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "mesaj_tipi", nullable = false, length = 20)
    @Builder.Default
    private MesajTipi mesajTipi = MesajTipi.TEXT;

    @Column(name = "icerik", columnDefinition = "TEXT", nullable = false)
    private String icerik;

    @Column(name = "whatsapp_message_id", length = 100)
    private String whatsappMessageId;

    @Column(name = "olusturma_tarihi")
    @Builder.Default
    private LocalDateTime olusturmaTarihi = LocalDateTime.now();

    public enum SenderType {
        CUSTOMER,   // Musteri
        AI,         // AI asistan
        STAFF,      // Calisan
        SYSTEM      // Sistem mesaji
    }

    public enum MesajTipi {
        TEXT,
        IMAGE,
        DOCUMENT,
        LOCATION,
        TEMPLATE
    }
}
