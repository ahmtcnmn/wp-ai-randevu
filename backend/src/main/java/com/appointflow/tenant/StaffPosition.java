package com.appointflow.tenant;

import java.util.List;
import java.util.Map;

/**
 * Sektör-bazlı çalışan pozisyonları. Role (OWNER/STAFF) sistem rolüdür;
 * pozisyon işletme içi unvandır ("saç yıkamacı", "manikür ustası" vb.).
 *
 * Nullable bir field — pozisyon zorunlu değil. Sadece UI'da daha açıklayıcı
 * göstermek için kullanılır (örn. randevu detayında "Ali — Saç Yıkamacı").
 */
public enum StaffPosition {
    // Berber
    USTA, CIRAK, SAC_YIKAYICI, BAYAN_BERBERI,
    // Kuaför / güzellik salonu
    KUAFOR, MANIKUR_PEDIKUR_USTASI, MAKYOZ, KAS_TASARIMCISI, CILT_BAKIM_USTASI, LAZER_OPERATORU,
    // Diş kliniği
    DIS_HEKIMI, AGIZ_DISC_SAGLIGI_TEKNISYENI, HIJYENIST,
    // Spa
    MASOR, MASOZ, ESTETISYEN, SPA_TERAPISTI,
    // Spor salonu
    ANTRENOR, PERSONAL_TRAINER, REHABILITASYON_UZMANI, GRUP_DERSI_EGITMENI,
    // Veteriner
    VETERINER, VETERINER_TEKNISYENI, PET_BAKIM_UZMANI,
    // Pet kuaförü
    PET_KUAFORU, KOPEK_EGITMENI,
    // Tırnak salonu
    NAIL_ARTIST, AKRILIK_USTASI,
    // Dövme stüdyosu
    DOVME_SANATCISI, PIERCING_USTASI,
    // Genel
    RESEPSIYONIST, YONETICI, TEMIZLIK_PERSONELI, DIGER
    ;

    /** Display ad — UI'da Türkçe gösterilir. */
    public String displayName() {
        return DISPLAY_NAMES.getOrDefault(this, name());
    }

    private static final Map<StaffPosition, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry(USTA, "Usta"),
            Map.entry(CIRAK, "Çırak"),
            Map.entry(SAC_YIKAYICI, "Saç Yıkamacı"),
            Map.entry(BAYAN_BERBERI, "Bayan Berberi"),
            Map.entry(KUAFOR, "Kuaför"),
            Map.entry(MANIKUR_PEDIKUR_USTASI, "Manikür/Pedikür Ustası"),
            Map.entry(MAKYOZ, "Makyöz"),
            Map.entry(KAS_TASARIMCISI, "Kaş Tasarımcısı"),
            Map.entry(CILT_BAKIM_USTASI, "Cilt Bakım Ustası"),
            Map.entry(LAZER_OPERATORU, "Lazer Operatörü"),
            Map.entry(DIS_HEKIMI, "Diş Hekimi"),
            Map.entry(AGIZ_DISC_SAGLIGI_TEKNISYENI, "Ağız-Diş Sağlığı Teknisyeni"),
            Map.entry(HIJYENIST, "Hijyenist"),
            Map.entry(MASOR, "Masör"),
            Map.entry(MASOZ, "Masöz"),
            Map.entry(ESTETISYEN, "Estetisyen"),
            Map.entry(SPA_TERAPISTI, "Spa Terapisti"),
            Map.entry(ANTRENOR, "Antrenör"),
            Map.entry(PERSONAL_TRAINER, "Personal Trainer"),
            Map.entry(REHABILITASYON_UZMANI, "Rehabilitasyon Uzmanı"),
            Map.entry(GRUP_DERSI_EGITMENI, "Grup Dersi Eğitmeni"),
            Map.entry(VETERINER, "Veteriner"),
            Map.entry(VETERINER_TEKNISYENI, "Veteriner Teknisyeni"),
            Map.entry(PET_BAKIM_UZMANI, "Pet Bakım Uzmanı"),
            Map.entry(PET_KUAFORU, "Pet Kuaförü"),
            Map.entry(KOPEK_EGITMENI, "Köpek Eğitmeni"),
            Map.entry(NAIL_ARTIST, "Nail Artist"),
            Map.entry(AKRILIK_USTASI, "Akrilik Ustası"),
            Map.entry(DOVME_SANATCISI, "Dövme Sanatçısı"),
            Map.entry(PIERCING_USTASI, "Piercing Ustası"),
            Map.entry(RESEPSIYONIST, "Resepsiyonist"),
            Map.entry(YONETICI, "Yönetici"),
            Map.entry(TEMIZLIK_PERSONELI, "Temizlik Personeli"),
            Map.entry(DIGER, "Diğer")
    );

    /** Sektör → o sektörde geçerli pozisyon listesi. */
    public static List<StaffPosition> forBusinessType(BusinessType type) {
        if (type == null) return GENEL_POZISYONLAR;
        return switch (type) {
            case BARBER -> List.of(USTA, CIRAK, SAC_YIKAYICI, BAYAN_BERBERI, RESEPSIYONIST, YONETICI, DIGER);
            case HAIR_SALON -> List.of(
                    KUAFOR, USTA, CIRAK, SAC_YIKAYICI, MANIKUR_PEDIKUR_USTASI, MAKYOZ, KAS_TASARIMCISI,
                    RESEPSIYONIST, YONETICI, DIGER);
            case BEAUTY_SALON -> List.of(
                    CILT_BAKIM_USTASI, MANIKUR_PEDIKUR_USTASI, MAKYOZ, KAS_TASARIMCISI, LAZER_OPERATORU,
                    ESTETISYEN, RESEPSIYONIST, YONETICI, DIGER);
            case DENTAL_CLINIC -> List.of(
                    DIS_HEKIMI, AGIZ_DISC_SAGLIGI_TEKNISYENI, HIJYENIST,
                    RESEPSIYONIST, YONETICI, DIGER);
            case SPA -> List.of(MASOR, MASOZ, ESTETISYEN, SPA_TERAPISTI, RESEPSIYONIST, YONETICI, DIGER);
            case GYM -> List.of(
                    ANTRENOR, PERSONAL_TRAINER, REHABILITASYON_UZMANI, GRUP_DERSI_EGITMENI,
                    RESEPSIYONIST, YONETICI, DIGER);
            case VETERINARY -> List.of(VETERINER, VETERINER_TEKNISYENI, PET_BAKIM_UZMANI,
                    RESEPSIYONIST, YONETICI, DIGER);
            case PET_GROOMING -> List.of(PET_KUAFORU, KOPEK_EGITMENI, PET_BAKIM_UZMANI,
                    RESEPSIYONIST, YONETICI, DIGER);
            case NAIL_SALON -> List.of(NAIL_ARTIST, MANIKUR_PEDIKUR_USTASI, AKRILIK_USTASI,
                    RESEPSIYONIST, YONETICI, DIGER);
            case TATTOO -> List.of(DOVME_SANATCISI, PIERCING_USTASI, RESEPSIYONIST, YONETICI, DIGER);
            case OTHER -> GENEL_POZISYONLAR;
        };
    }

    private static final List<StaffPosition> GENEL_POZISYONLAR = List.of(
            USTA, CIRAK, RESEPSIYONIST, YONETICI, TEMIZLIK_PERSONELI, DIGER);
}
