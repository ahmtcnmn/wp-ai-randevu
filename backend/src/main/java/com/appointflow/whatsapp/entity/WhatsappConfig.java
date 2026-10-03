package com.appointflow.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "phone_number_id", length = 50)
    private String phoneNumberId;

    @Column(name = "waba_id", length = 50)
    private String wabaId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "verify_token", length = 100)
    private String verifyToken;

    @Column(name = "app_secret", length = 200)
    private String appSecret;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "display_phone", length = 20)
    private String displayPhone;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = false;

    @Column(name = "olusturma_tarihi")
    @Builder.Default
    private LocalDateTime olusturmaTarihi = LocalDateTime.now();

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PreUpdate
    protected void onUpdate() {
        this.guncellemeTarihi = LocalDateTime.now();
    }
}
