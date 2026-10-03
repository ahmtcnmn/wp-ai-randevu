/**
 * Audit log action enum'larının Türkçe etiketleri.
 * Backend `GET /api/v1/audit-logs/actions` ile aynı map'i döner; fallback olarak burası kullanılır.
 */
export const AUDIT_ACTION_LABELS: Record<string, string> = {
  LOGIN: "Giriş yapıldı",
  LOGIN_2FA_REQUIRED: "Giriş 2FA bekliyor",
  LOGIN_2FA_FAILED: "2FA başarısız",
  LOGIN_BLOCKED_EMAIL_UNVERIFIED: "Giriş engellendi (e-posta doğrulanmamış)",
  LOGIN_WRONG_APP: "Yanlış uygulamadan giriş denemesi",
  LOGOUT: "Çıkış yapıldı",
  REGISTER: "Yeni hesap oluşturuldu",
  PASSWORD_CHANGE: "Şifre değiştirildi",
  PASSWORD_RESET_REQUEST: "Şifre sıfırlama istendi",
  PASSWORD_RESET_COMPLETE: "Şifre sıfırlandı",
  EMAIL_VERIFIED: "E-posta doğrulandı",
  TWO_FACTOR_ENABLED: "2FA aktif edildi",
  TWO_FACTOR_DISABLED: "2FA devre dışı",
  USER_CREATE: "Çalışan oluşturuldu",
  USER_UPDATE: "Çalışan güncellendi",
  USER_DELETE: "Çalışan pasifleştirildi",
  APPOINTMENT_CREATE: "Randevu oluşturuldu",
  APPOINTMENT_UPDATE: "Randevu güncellendi",
  APPOINTMENT_CANCEL: "Randevu iptal edildi",
  APPOINTMENT_STATUS_CHANGE: "Randevu durumu değişti",
  APPOINTMENT_PRODUCT_ADDED: "Randevuya ürün eklendi",
  CUSTOMER_CREATE: "Müşteri eklendi",
  CUSTOMER_UPDATE: "Müşteri güncellendi",
  CUSTOMER_DELETE: "Müşteri silindi",
  SUBSCRIPTION_CREATE: "Abonelik oluşturuldu",
  SUBSCRIPTION_UPGRADE: "Abonelik yükseltildi",
  SUBSCRIPTION_CANCEL: "Abonelik iptal edildi",
  PAYMENT_SUCCESS: "Ödeme başarılı",
  PAYMENT_FAILED: "Ödeme başarısız",
  ACCOUNT_DELETE_REQUEST: "Hesap silme talebi",
  ACCOUNT_RESTORE: "Hesap geri yüklendi",
  WHATSAPP_CONFIG_UPDATE: "WhatsApp ayarları güncellendi",
  AI_CONFIG_UPDATE: "AI ayarları güncellendi",
  CAMPAIGN_CREATE: "Kampanya oluşturuldu",
  REMINDER_TEMPLATE_CREATE: "Hatırlatma şablonu oluşturuldu",
};

export function auditActionLabel(action: string | null | undefined): string {
  if (!action) return "—";
  return AUDIT_ACTION_LABELS[action] ?? action;
}
