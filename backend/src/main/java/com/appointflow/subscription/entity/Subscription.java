package com.appointflow.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIALING;

    @Column(name = "iyzico_subscription_reference_code", unique = true)
    private String iyzicoSubscriptionReferenceCode;

    @Column(name = "iyzico_customer_reference_code")
    private String iyzicoCustomerReferenceCode;

    @Column(name = "baslangic_tarihi", nullable = false)
    private LocalDateTime baslangicTarihi;

    @Column(name = "bitis_tarihi")
    private LocalDateTime bitisTarihi;

    @Column(name = "deneme_bitis_tarihi")
    private LocalDateTime denemeBitisTarihi;

    @Column(name = "sonraki_odeme_tarihi")
    private LocalDateTime sonrakiOdemeTarihi;

    @Column(name = "iptal_tarihi")
    private LocalDateTime iptalTarihi;

    @Column(name = "iptal_nedeni", columnDefinition = "TEXT")
    private String iptalNedeni;

    @Column(name = "grace_period_bitis")
    private LocalDateTime gracePeriodBitis;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
