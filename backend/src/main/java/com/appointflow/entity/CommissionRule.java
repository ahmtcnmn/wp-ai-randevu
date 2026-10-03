package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "staff_id")
    private Long staffId; // null = tenant default

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false)
    private CommissionType commissionType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private CommissionScope scope = CommissionScope.SERVICE;

    /** Sadece scope=PRODUCT ile birlikte kullanilir. null = tum urunler */
    @Column(name = "product_id")
    private Long productId;

    /** Sadece scope=PRODUCT ile birlikte kullanilir. null = tum kategoriler */
    @Column(name = "product_kategori", length = 100)
    private String productKategori;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate; // 0.0 - 1.0

    @Column(name = "bonus_threshold", precision = 10, scale = 2)
    private BigDecimal bonusThreshold;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

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
