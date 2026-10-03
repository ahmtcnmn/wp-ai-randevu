package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_service_suggestions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "hizmet_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductServiceSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hizmet_id", nullable = false)
    private Hizmet hizmet;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
