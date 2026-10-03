package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReminderTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false)
    private String mesaj;

    // Opsiyonel — hizmet bazlı varsayılan şablon
    @Column(name = "hizmet_id")
    private Long hizmetId;

    @Builder.Default
    @Column(nullable = false)
    private Integer gunSonra = 30;

    /** true = randevudan {gunSonra} {birim} ONCE, false = SONRA. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean oncesi = false;

    /** Birim: GUN veya SAAT. Cron logic'inde offset hesaplaması için. */
    @Builder.Default
    @Column(nullable = false, length = 10)
    private String birim = "GUN";

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String kanal = "WHATSAPP"; // WHATSAPP | SMS | EMAIL

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
