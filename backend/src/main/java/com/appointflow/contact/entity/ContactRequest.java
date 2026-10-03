package com.appointflow.contact.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ad;

    @Column(nullable = false, length = 100)
    private String soyad;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 20)
    private String telefon;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String durum = "YENI"; // YENI | ARANDI | DONUS_BEKLENIYOR | KAPANDI

    @Column(name = "super_admin_notu", columnDefinition = "TEXT")
    private String superAdminNotu;

    @Column(name = "cevaplayan_user_id")
    private Long cevaplayanUserId;

    @Column(name = "cevaplanma_tarihi")
    private LocalDateTime cevaplanmaTarihi;

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
