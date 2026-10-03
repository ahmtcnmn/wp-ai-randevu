package com.appointflow.ai.intent;

public enum Intent {
    BOOK_APPOINTMENT,      // Randevu alma
    CANCEL_APPOINTMENT,    // Randevu iptal
    QUERY_APPOINTMENT,     // Randevu sorgulama
    RESCHEDULE,            // Yeniden planlama
    SERVICE_INFO,          // Hizmet / fiyat / sure bilgisi
    PRODUCT_INQUIRY,       // Urun sorusu
    COMPLAINT,             // Sikayet -> otomatik handoff
    STAFF_REQUEST,         // Belirli calisan istegi -> handoff
    GENERAL_CHAT           // Genel sohbet / tanimsiz
}
