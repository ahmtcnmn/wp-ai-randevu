package com.appointflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_reminders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "randevu_id", nullable = false)
    private Randevu randevu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Hangi şablondan türetildi (nullable — manuel mesajla da oluşturulabilir)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ReminderTemplate template;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mesaj;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String kanal = "WHATSAPP"; // WHATSAPP | SMS | EMAIL

    // Gönderilecek gün
    @Column(name = "gonderim_tarihi", nullable = false)
    private LocalDate gonderimTarihi;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // SNOOZED ise yeni gönderim tarihi
    @Column(name = "snoozed_until")
    private LocalDate snoozedUntil;

    // Kaç kez ertelendi
    @Builder.Default
    @Column(name = "snooze_count", nullable = false)
    private Integer snoozeCount = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
