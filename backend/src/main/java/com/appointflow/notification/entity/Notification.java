package com.appointflow.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String tip; // APPOINTMENT_CREATED, APPOINTMENT_CANCELLED, QUOTA_WARNING, PAYMENT_SUCCESS, ...

    @Column(nullable = false, length = 200)
    private String baslik;

    @Column(columnDefinition = "TEXT")
    private String icerik;

    @Column(length = 500)
    private String link;

    @Builder.Default
    @Column(nullable = false)
    private Boolean okundu = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
