package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "segment_campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SegmentCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "baslik", nullable = false)
    private String baslik;

    @Enumerated(EnumType.STRING)
    @Column(name = "hedef_segment", nullable = false)
    private SegmentType hedefSegment;

    @Column(name = "mesaj", nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    // Kaç kişiye gönderildi
    @Builder.Default
    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    // Kaç kişi yanıt verdi (dönüşüm)
    @Builder.Default
    @Column(name = "response_count", nullable = false)
    private Integer responseCount = 0;

    // Kampanyayı başlatan çalışan
    @Column(name = "created_by_id")
    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.ACTIVE;

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
