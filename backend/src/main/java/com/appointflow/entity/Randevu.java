package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "randevular")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Randevu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne
    @JoinColumn(name = "musteri_id")
    private Kullanici musteri;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "uzman_id", nullable = false)
    private Kullanici uzman;

    @ManyToOne
    @JoinColumn(name = "hizmet_id", nullable = false)
    private Hizmet hizmet;

    @Column(nullable = false)
    private LocalDateTime tarihSaat;

    @Column(nullable = false)
    private LocalDateTime bitisTarihi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RandevuDurumu durum;

    @Column(name = "randevu_notu")
    private String not;

    @Column(name = "odenen_tutar")
    private Double odenenTutar;

    @Builder.Default
    @Column(name = "indirim_uygulandi_mi", nullable = false)
    private Boolean indirimUygulandiMi = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private RandevuKaynak kaynak = RandevuKaynak.MANUAL;

    private Double toplamFiyat;
    private Integer toplamSureDk;
    private String iptalNedeni;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "randevu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentServiceEntity> hizmetler;

    @OneToMany(mappedBy = "randevu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentNote> notlar;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
