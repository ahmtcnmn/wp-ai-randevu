package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "randevu_id", nullable = false)
    private Randevu randevu;

    @ManyToOne
    @JoinColumn(name = "yazan_id", nullable = false)
    private Kullanici yazan;

    @Column(nullable = false)
    private String icerik;

    @Builder.Default
    @Column(nullable = false)
    private String tur = "INTERNAL"; // INTERNAL, MUSTERI, REMINDER

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
