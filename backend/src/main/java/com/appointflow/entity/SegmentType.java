package com.appointflow.entity;

public enum SegmentType {
    NEW,         // İlk randevusu 30 gün içinde, toplam ≤ 2 randevu
    REGULAR,     // 30 günde en az 1 randevu, son 60 günde aktif
    LOYAL,       // 6+ ay müşteri, 10+ randevu veya sadakat puanı ≥ 500
    OCCASIONAL,  // 60-120 günde bir randevu
    DRIFTING,    // Son 60-120 gün arasında sessiz
    LOST,        // 120+ gün hiç gelmemiş
    VIP,         // Ortalama harcama ≥ tenant ortalaması × 2
    AT_RISK      // Gelmeme sayısı ≥ 2 veya son 3 randevudan 2'si GELMEDI
}
