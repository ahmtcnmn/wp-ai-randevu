package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "earnings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Earning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "randevu_id", nullable = false)
    private Randevu randevu;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "commission_rule_id")
    private Long commissionRuleId;

    @Column(name = "earning_period_id")
    private Long earningPeriodId;

    @Column(name = "product_sale_id")
    private Long productSaleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false)
    private CommissionType commissionType;

    @Column(name = "rate_snapshot", nullable = false, precision = 5, scale = 4)
    private BigDecimal rateSnapshot;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EarningStatus status = EarningStatus.PENDING;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
