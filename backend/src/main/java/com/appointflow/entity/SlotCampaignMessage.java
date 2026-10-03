package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "slot_campaign_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlotCampaignMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private SlotCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // SENT, RESPONDED_YES, RESPONDED_NO, SLOT_TAKEN
    @Builder.Default
    @Column(nullable = false)
    private String durum = "SENT";

    @Builder.Default
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
