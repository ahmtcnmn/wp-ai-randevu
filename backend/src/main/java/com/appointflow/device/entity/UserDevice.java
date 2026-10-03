package com.appointflow.device.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(name = "user_devices_token_unique", columnNames = {"user_id", "expo_push_token"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expo_push_token", nullable = false)
    private String expoPushToken;

    @Column(length = 20)
    private String platform;

    @Column(name = "son_kullanim")
    private LocalDateTime sonKullanim;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
