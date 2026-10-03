package com.appointflow.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AuditService.log() çağrılarında kullanılan tüm action enum-string'leri için
 * Türkçe display etiketleri. Frontend audit log sayfası bu map'i çekip kullanıcıya
 * raw enum yerine "Giriş Yapıldı" gibi okunabilir text gösterir.
 */
public final class AuditActionLabels {

    private AuditActionLabels() {}

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        // Auth
        LABELS.put("LOGIN", "Giriş yapıldı");
        LABELS.put("LOGIN_2FA_REQUIRED", "Giriş 2FA bekliyor");
        LABELS.put("LOGIN_2FA_FAILED", "2FA başarısız");
        LABELS.put("LOGIN_BLOCKED_EMAIL_UNVERIFIED", "Giriş engellendi (e-posta doğrulanmamış)");
        LABELS.put("LOGIN_WRONG_APP", "Yanlış uygulamadan giriş denemesi");
        LABELS.put("LOGOUT", "Çıkış yapıldı");
        LABELS.put("REGISTER", "Yeni hesap oluşturuldu");
        LABELS.put("PASSWORD_CHANGE", "Şifre değiştirildi");
        LABELS.put("PASSWORD_RESET_REQUEST", "Şifre sıfırlama istendi");
        LABELS.put("PASSWORD_RESET_COMPLETE", "Şifre sıfırlandı");
        LABELS.put("EMAIL_VERIFIED", "E-posta doğrulandı");
        // 2FA
        LABELS.put("TWO_FACTOR_ENABLED", "2FA aktif edildi");
        LABELS.put("TWO_FACTOR_DISABLED", "2FA devre dışı");
        // User
        LABELS.put("USER_CREATE", "Çalışan oluşturuldu");
        LABELS.put("USER_UPDATE", "Çalışan güncellendi");
        LABELS.put("USER_DELETE", "Çalışan pasifleştirildi");
        // Appointment
        LABELS.put("APPOINTMENT_CREATE", "Randevu oluşturuldu");
        LABELS.put("APPOINTMENT_UPDATE", "Randevu güncellendi");
        LABELS.put("APPOINTMENT_CANCEL", "Randevu iptal edildi");
        LABELS.put("APPOINTMENT_STATUS_CHANGE", "Randevu durumu değişti");
        LABELS.put("APPOINTMENT_PRODUCT_ADDED", "Randevuya ürün eklendi");
        // Customer
        LABELS.put("CUSTOMER_CREATE", "Müşteri eklendi");
        LABELS.put("CUSTOMER_UPDATE", "Müşteri güncellendi");
        LABELS.put("CUSTOMER_DELETE", "Müşteri silindi");
        // Billing
        LABELS.put("SUBSCRIPTION_CREATE", "Abonelik oluşturuldu");
        LABELS.put("SUBSCRIPTION_UPGRADE", "Abonelik yükseltildi");
        LABELS.put("SUBSCRIPTION_CANCEL", "Abonelik iptal edildi");
        LABELS.put("PAYMENT_SUCCESS", "Ödeme başarılı");
        LABELS.put("PAYMENT_FAILED", "Ödeme başarısız");
        // Account
        LABELS.put("ACCOUNT_DELETE_REQUEST", "Hesap silme talebi");
        LABELS.put("ACCOUNT_RESTORE", "Hesap geri yüklendi");
        // Misc
        LABELS.put("WHATSAPP_CONFIG_UPDATE", "WhatsApp ayarları güncellendi");
        LABELS.put("AI_CONFIG_UPDATE", "AI ayarları güncellendi");
        LABELS.put("CAMPAIGN_CREATE", "Kampanya oluşturuldu");
        LABELS.put("REMINDER_TEMPLATE_CREATE", "Hatırlatma şablonu oluşturuldu");
    }

    public static Map<String, String> all() {
        return LABELS;
    }

    public static String label(String action) {
        return LABELS.getOrDefault(action, action);
    }
}
