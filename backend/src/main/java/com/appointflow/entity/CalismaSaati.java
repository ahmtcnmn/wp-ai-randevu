package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "calisma_saatleri")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalismaSaati {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "uzman_id", nullable = false)
    private Kullanici uzman;

    // 1: Pazartesi, 2: Salı... 7: Pazar
    @Column(nullable = false)
    private Integer gunOfWeek;

    @Column(nullable = false)
    private LocalTime baslangicSaat;

    @Column(nullable = false)
    private LocalTime bitisSaat;

    @Column(nullable = false)
    private Boolean kapaliMi;
}
