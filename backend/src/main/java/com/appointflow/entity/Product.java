package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String ad;

    private String aciklama;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fiyat;

    @Builder.Default
    @Column(nullable = false)
    private Integer stok = 0;

    private String kategori;

    @Builder.Default
    @Column(name = "ai_oneri_aktif", nullable = false)
    private Boolean aiOneriAktif = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductServiceSuggestion> serviceSuggestions;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
