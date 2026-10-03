package com.appointflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hizmetler")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hizmet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false)
    private String ad;

    private String aciklama;

    @Column(nullable = false)
    private Integer sureDakika;

    @Column(nullable = false)
    private Double fiyat;

    @ManyToOne
    @JoinColumn(name = "kategori_id")
    private ServiceCategory kategori;

    @Builder.Default
    @Column(nullable = false)
    private Integer bufferOnceDk = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer bufferSonraDk = 0;

    @Builder.Default
    @Column(nullable = false)
    private String takvimRengi = "#3B82F6";

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "hizmet", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Randevu> randevular;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
