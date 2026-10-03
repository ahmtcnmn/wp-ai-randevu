package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "earning_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EarningPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Builder.Default
    @Column(name = "total_gross", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_net", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EarningStatus status = EarningStatus.PENDING;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

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
