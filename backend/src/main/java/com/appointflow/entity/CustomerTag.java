package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_tags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String etiket;
}
