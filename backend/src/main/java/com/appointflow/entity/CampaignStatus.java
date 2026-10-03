package com.appointflow.entity;

public enum CampaignStatus {
    ACTIVE,     // Mesajlar gönderildi, yanıt bekleniyor
    FILLED,     // Slot dolu, kampanya kapandı
    EXPIRED,    // 30dk geçti, yanıt gelmedi
    CANCELLED,  // Manuel iptal
    COMPLETED   // Segment kampanyası tamamlandı
}
