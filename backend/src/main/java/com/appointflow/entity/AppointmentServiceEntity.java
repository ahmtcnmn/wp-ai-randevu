package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_services")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "randevu_id", nullable = false)
    private Randevu randevu;

    @ManyToOne
    @JoinColumn(name = "hizmet_id", nullable = false)
    private Hizmet hizmet;

    @Column(nullable = false)
    private Double fiyatSnapshot;

    @Column(name = "sure_dk", nullable = false)
    private Integer sureDk;
}
