package com.appointflow.entity;

public enum ReminderStatus {
    PENDING,        // Henüz gönderilmedi
    SENT,           // Mesaj gönderildi, yanıt bekleniyor
    RESPONDED_YES,  // Müşteri evet dedi → AI randevu akışını başlattı
    RESPONDED_NO,   // Müşteri hayır dedi → kapatıldı
    SNOOZED,        // Müşteri tarih verdi → ertelendi
    CANCELLED       // İptal edildi
}
