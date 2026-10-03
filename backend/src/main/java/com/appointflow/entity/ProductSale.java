package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "randevu_id")
    private Randevu randevu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false)
    private Integer adet;

    @Column(name = "birim_fiyat_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyatSnapshot;

    @Column(name = "toplam_tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal toplamTutar;

    @Column(name = "commission_rate_snapshot", precision = 5, scale = 4)
    private BigDecimal commissionRateSnapshot;

    @Column(name = "commission_amount", precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
