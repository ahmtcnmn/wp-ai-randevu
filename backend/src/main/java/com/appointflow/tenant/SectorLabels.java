package com.appointflow.tenant;

import java.util.Map;

/**
 * Sektör-spesifik etiket sözlüğü.
 * Frontend (web + mobil) bu sözlüğü çağırıp UI'da "Uzman" yerine "Berber/Doktor/Antrenör" gösterir.
 *
 * Yeni sektör eklemek için:
 * 1. {@link BusinessType}'a enum değeri ekle
 * 2. {@link #LABELS} map'ine etiket sözlüğü ekle
 * 3. {@link #DISPLAY_NAMES} map'ine işletme tipi adını ekle
 */
public final class SectorLabels {

    private SectorLabels() {}

    /** İşletme tipinin kullanıcı dostu adı (login hatasında, kayıt ekranında vb. gösterilir). */
    private static final Map<BusinessType, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry(BusinessType.BARBER, "Erkek Berber Salonu"),
            Map.entry(BusinessType.HAIR_SALON, "Kadın Kuaförü"),
            Map.entry(BusinessType.DENTAL_CLINIC, "Diş Kliniği"),
            Map.entry(BusinessType.BEAUTY_SALON, "Güzellik Salonu"),
            Map.entry(BusinessType.SPA, "Spa / Masaj Salonu"),
            Map.entry(BusinessType.GYM, "Spor Salonu"),
            Map.entry(BusinessType.VETERINARY, "Veteriner Kliniği"),
            Map.entry(BusinessType.PET_GROOMING, "Pet Kuaförü"),
            Map.entry(BusinessType.NAIL_SALON, "Tırnak Salonu"),
            Map.entry(BusinessType.TATTOO, "Dövme Stüdyosu"),
            Map.entry(BusinessType.OTHER, "İşletme")
    );

    /** Her sektör için: staffSingular/Plural, customerSingular/Plural, serviceSingular/Plural, appointmentSingular/Plural */
    private static final Map<BusinessType, LabelDictionary> LABELS = Map.ofEntries(
            Map.entry(BusinessType.BARBER, LabelDictionary.of(
                    "Berber", "Berberler",
                    "Müşteri", "Müşteriler",
                    "Hizmet", "Hizmetler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.HAIR_SALON, LabelDictionary.of(
                    "Kuaför", "Kuaförler",
                    "Müşteri", "Müşteriler",
                    "Hizmet", "Hizmetler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.DENTAL_CLINIC, LabelDictionary.of(
                    "Doktor", "Doktorlar",
                    "Hasta", "Hastalar",
                    "Tedavi", "Tedaviler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.BEAUTY_SALON, LabelDictionary.of(
                    "Uzman", "Uzmanlar",
                    "Müşteri", "Müşteriler",
                    "Hizmet", "Hizmetler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.SPA, LabelDictionary.of(
                    "Terapist", "Terapistler",
                    "Müşteri", "Müşteriler",
                    "Uygulama", "Uygulamalar",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.GYM, LabelDictionary.of(
                    "Antrenör", "Antrenörler",
                    "Üye", "Üyeler",
                    "Ders", "Dersler",
                    "Seans", "Seanslar"
            )),
            Map.entry(BusinessType.VETERINARY, LabelDictionary.of(
                    "Veteriner", "Veterinerler",
                    "Hasta Sahibi", "Hasta Sahipleri",
                    "Muayene", "Muayeneler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.PET_GROOMING, LabelDictionary.of(
                    "Pet Kuaförü", "Pet Kuaförleri",
                    "Pet Sahibi", "Pet Sahipleri",
                    "Bakım", "Bakımlar",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.NAIL_SALON, LabelDictionary.of(
                    "Tırnak Uzmanı", "Tırnak Uzmanları",
                    "Müşteri", "Müşteriler",
                    "İşlem", "İşlemler",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.TATTOO, LabelDictionary.of(
                    "Sanatçı", "Sanatçılar",
                    "Müşteri", "Müşteriler",
                    "Tasarım", "Tasarımlar",
                    "Randevu", "Randevular"
            )),
            Map.entry(BusinessType.OTHER, LabelDictionary.of(
                    "Çalışan", "Çalışanlar",
                    "Müşteri", "Müşteriler",
                    "Hizmet", "Hizmetler",
                    "Randevu", "Randevular"
            ))
    );

    public static String getDisplayName(BusinessType type) {
        return DISPLAY_NAMES.getOrDefault(type, "İşletme");
    }

    public static LabelDictionary getLabels(BusinessType type) {
        return LABELS.getOrDefault(type, LABELS.get(BusinessType.OTHER));
    }

    /** Etiket sözlüğü — frontend bunu kullanarak UI'da etiket render eder. */
    public record LabelDictionary(
            String staffSingular,
            String staffPlural,
            String customerSingular,
            String customerPlural,
            String serviceSingular,
            String servicePlural,
            String appointmentSingular,
            String appointmentPlural
    ) {
        static LabelDictionary of(
                String staffS, String staffP,
                String customerS, String customerP,
                String serviceS, String serviceP,
                String appointmentS, String appointmentP) {
            return new LabelDictionary(staffS, staffP, customerS, customerP,
                    serviceS, serviceP, appointmentS, appointmentP);
        }
    }
}
