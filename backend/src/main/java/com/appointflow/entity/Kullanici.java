package com.appointflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "kullanicilar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false)
    private String soyad;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String sifre;

    @Column(nullable = false)
    private String telefon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role rol;

    @ManyToOne
    @JoinColumn(name = "sube_id")
    private Sube sube;

    @Enumerated(EnumType.STRING)
    @Column(name = "pozisyon", length = 64)
    private com.appointflow.tenant.StaffPosition pozisyon;

    @Builder.Default
    @Column(nullable = false)
    private Integer gelmemeSayisi = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean karaListedeMi = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer sadakatPuani = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Builder.Default
    @Column(name = "email_dogrulandi", nullable = false)
    private Boolean emailDogrulandi = false;

    @JsonIgnore
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @JsonIgnore
    @Column(name = "two_factor_pending_secret")
    private String twoFactorPendingSecret;

    private LocalDateTime sonGirisTarihi;

    /** Soft delete timestamp. NULL = aktif kayıt. 30 gün sonra hard delete cron'u siler. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "musteri", cascade = CascadeType.ALL)
    private List<Randevu> randevular;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
