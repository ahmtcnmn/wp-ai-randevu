package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false)
    private String soyad;

    @Column(nullable = false)
    private String telefon;

    private String email;

    private String notlar;

    @Builder.Default
    @Column(nullable = false)
    private Integer gelmemeSayisi = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean karaListedeMi = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer sadakatPuani = 0;

    private LocalDateTime sonZiyaret;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
