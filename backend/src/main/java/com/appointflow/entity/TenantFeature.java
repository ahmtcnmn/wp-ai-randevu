package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_features", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "feature_key"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String featureKey;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aktif = false;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
