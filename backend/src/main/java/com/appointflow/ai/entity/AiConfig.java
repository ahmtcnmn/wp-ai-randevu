package com.appointflow.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "persona_adi", length = 100)
    private String personaAdi;

    @Column(name = "sistem_promptu", columnDefinition = "TEXT")
    private String sistemPromptu;

    /**
     * Onboarding'de kullanıcının "İşletmeniz hakkında" alanına yazdığı serbest metin.
     * AI prompt'larında işletmeyi tanıtmak için kullanılır.
     */
    @Column(name = "isletme_aciklamasi", columnDefinition = "TEXT")
    private String isletmeAciklamasi;

    @Column(name = "dil", length = 10)
    @Builder.Default
    private String dil = "tr";

    @Column(name = "fiyat_bilgisi_goster")
    @Builder.Default
    private Boolean fiyatBilgisiGoster = true;

    @Column(name = "otomatik_onay")
    @Builder.Default
    private Boolean otomatikOnay = false;

    @Column(name = "handoff_kelimeleri", columnDefinition = "TEXT")
    @Builder.Default
    private String handoffKelimeleri = "şikayet,yanlış,berbat,kötü,rezalet,müdür,patron,sahibi";

    @Column(name = "max_token")
    @Builder.Default
    private Integer maxToken = 1024;

    @Column(name = "model", length = 50)
    @Builder.Default
    private String model = "gpt-4o-mini";

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

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
