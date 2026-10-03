package com.appointflow.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Column(name = "iyzico_payment_id", unique = true)
    private String iyzicoPaymentId;

    @Column(name = "iyzico_token")
    private String iyzicoToken;

    @Column(nullable = false)
    private BigDecimal tutar;

    @Column(name = "para_birimi", nullable = false)
    @Builder.Default
    private String paraBirimi = "TRY";

    @Column(nullable = false)
    private String durum;

    @Column(name = "donem_baslangic", nullable = false)
    private LocalDate donemBaslangic;

    @Column(name = "donem_bitis", nullable = false)
    private LocalDate donemBitis;

    @Column(name = "odeme_tarihi")
    private LocalDateTime odemeTarihi;

    @Column(name = "hata_mesaji", columnDefinition = "TEXT")
    private String hataMesaji;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
