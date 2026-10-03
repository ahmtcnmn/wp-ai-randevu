package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_segments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "customer_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false)
    private SegmentType segmentType;

    @Column(name = "total_appointments", nullable = false)
    private Integer totalAppointments;

    @Column(name = "total_spent", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "avg_spent_per_visit", precision = 10, scale = 2)
    private BigDecimal avgSpentPerVisit;

    @Column(name = "days_since_last_visit")
    private Integer daysSinceLastVisit;

    @Column(name = "no_show_count", nullable = false)
    private Integer noShowCount;

    @Builder.Default
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
