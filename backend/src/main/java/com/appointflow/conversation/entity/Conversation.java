package com.appointflow.conversation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    @Builder.Default
    private ConversationDurum durum = ConversationDurum.ACTIVE;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aktif_handler", length = 20)
    private ActiveHandler aktifHandler;

    @Column(name = "kanal", length = 20)
    @Builder.Default
    private String kanal = "WHATSAPP";

    @Column(name = "son_mesaj_zamani")
    private LocalDateTime sonMesajZamani;

    @Column(name = "olusturma_tarihi")
    @Builder.Default
    private LocalDateTime olusturmaTarihi = LocalDateTime.now();

    @Column(name = "kapatma_tarihi")
    private LocalDateTime kapatmaTarihi;

    public enum ConversationDurum {
        ACTIVE,        // AI yonetiyor
        WAITING,       // Calisana bildirim gitti, bekleniyor
        HUMAN_ACTIVE,  // Calisan veya Owner yonetiyor
        CLOSED         // Konusma tamamlandi
    }

    public enum ActiveHandler {
        BOOKING,       // BookingFlowHandler aktif
        CANCELLATION   // CancellationFlowHandler aktif
    }
}
