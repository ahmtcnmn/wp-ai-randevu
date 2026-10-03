package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_definitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String featureKey;

    @Column(nullable = false)
    private String ad;

    private String aciklama;

    @Builder.Default
    @Column(nullable = false)
    private Boolean varsayilan = false;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
