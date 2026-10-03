package com.appointflow.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "template_key", nullable = false, length = 50)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "kategori", nullable = false, length = 20)
    @Builder.Default
    private TemplateKategori kategori = TemplateKategori.UTILITY;

    @Column(name = "dil", length = 10)
    @Builder.Default
    private String dil = "tr";

    @Column(name = "icerik", columnDefinition = "TEXT", nullable = false)
    private String icerik;

    @Column(name = "header_text", length = 200)
    private String headerText;

    @Column(name = "footer_text", length = 200)
    private String footerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    @Builder.Default
    private TemplateDurum durum = TemplateDurum.DRAFT;

    @Column(name = "meta_template_id", length = 50)
    private String metaTemplateId;

    @Column(name = "red_nedeni", columnDefinition = "TEXT")
    private String redNedeni;

    @Column(name = "olusturma_tarihi")
    @Builder.Default
    private LocalDateTime olusturmaTarihi = LocalDateTime.now();

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;

    @PreUpdate
    protected void onUpdate() {
        this.guncellemeTarihi = LocalDateTime.now();
    }

    public enum TemplateKategori {
        UTILITY,
        MARKETING,
        AUTHENTICATION
    }

    public enum TemplateDurum {
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED,
        PAUSED,
        DISABLED
    }
}
