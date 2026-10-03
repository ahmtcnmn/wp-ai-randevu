package com.appointflow.entity;

import com.appointflow.tenant.BusinessType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ad;

    @Column(nullable = false, unique = true)
    private String slug;

    private String email;

    private String telefon;

    private String adres;

    private String sehir;

    @Column(length = 50)
    private String ulke; // Iyzico "Turkey" / "Germany" gibi tam ad veya ISO-3166-1

    private String tckn;

    private String logoUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = true;

    @Builder.Default
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 32)
    private BusinessType businessType = BusinessType.OTHER;

    @Column(name = "cancellation_policy_saat_once")
    private Integer cancellationPolicySaatOnce;

    @Column(name = "cancellation_policy_mesaj", columnDefinition = "TEXT")
    private String cancellationPolicyMesaj;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Soft delete timestamp. NULL = aktif. 30 gün sonra hard delete cron'u siler. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
