package com.appointflow.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_key", nullable = false, unique = true)
    private String planKey;

    @Column(nullable = false)
    private String ad;

    @Column(columnDefinition = "TEXT")
    private String aciklama;

    @Column(name = "aylik_fiyat", nullable = false)
    private BigDecimal aylikFiyat;

    @Column(name = "max_sube")
    private Integer maxSube;

    @Column(name = "max_calisan")
    private Integer maxCalisan;

    @Column(name = "max_aylik_randevu")
    private Integer maxAylikRandevu;

    @Column(name = "iyzico_pricing_plan_reference_code")
    private String iyzicoReferenceCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
