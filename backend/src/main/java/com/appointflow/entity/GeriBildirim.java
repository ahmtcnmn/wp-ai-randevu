package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "geri_bildirimler")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeriBildirim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "randevu_id", nullable = false)
    private Randevu randevu;

    @Column(nullable = false)
    private Integer puan; // 1 ile 5 arası

    @Column(length = 500)
    private String yorum;

    @Column(name = "sikayet_varmi", nullable = false)
    private Boolean sikayetVarmi; // Eğer 1-2 yıldız verilmişse veya müşteri özel olarak işaretlemişse true olur

    @Column(nullable = false)
    private LocalDateTime tarih;
}
