package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_recovery_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TwoFactorRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isUnused() {
        return usedAt == null;
    }
}
