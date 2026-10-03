package com.appointflow.conversation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_handoffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationHandoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "devralan_user_id")
    private Long devralanUserId;

    @Column(name = "devralan_ad", length = 100)
    private String devralanAd;

    @Column(name = "neden", length = 200)
    private String neden;

    @Column(name = "devralma_zamani")
    @Builder.Default
    private LocalDateTime devralmaZamani = LocalDateTime.now();

    @Column(name = "birakma_zamani")
    private LocalDateTime birakmaZamani;
}
