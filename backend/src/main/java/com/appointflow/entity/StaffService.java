package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_services", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"kullanici_id", "hizmet_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "kullanici_id", nullable = false)
    private Kullanici kullanici;

    @ManyToOne
    @JoinColumn(name = "hizmet_id", nullable = false)
    private Hizmet hizmet;
}
