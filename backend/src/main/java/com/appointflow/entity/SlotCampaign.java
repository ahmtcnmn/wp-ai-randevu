package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "slot_campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlotCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // İptal edilen randevu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_randevu_id", nullable = false)
    private Randevu cancelledRandevu;

    // Boşalan slot zamanı
    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    // Kampanya mesajı
    @Column(name = "mesaj", nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    // Kaç kişiye gönderildi
    @Builder.Default
    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.ACTIVE;

    // Slotu dolduran randevu (FILLED durumunda)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filled_randevu_id")
    private Randevu filledRandevu;

    @Builder.Default
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
