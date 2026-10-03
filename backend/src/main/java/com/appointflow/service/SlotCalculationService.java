package com.appointflow.service;

import com.appointflow.entity.*;
import com.appointflow.repository.CalismaSaatiRepository;
import com.appointflow.repository.HizmetRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.RandevuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotCalculationService {

    private final CalismaSaatiRepository calismaSaatiRepository;
    private final RandevuRepository randevuRepository;
    private final HizmetRepository hizmetRepository;
    private final KullaniciRepository kullaniciRepository;

    public List<LocalTime> getAvailableSlots(Long uzmanId, List<Long> hizmetIds, LocalDate tarih) {
        // Uzman aktif degilse slot dondurme
        Kullanici uzman = kullaniciRepository.findById(uzmanId).orElse(null);
        if (uzman == null || !Boolean.TRUE.equals(uzman.getAktif())) {
            return new ArrayList<>();
        }

        int gunOfWeek = tarih.getDayOfWeek().getValue();

        // Calisma saati kontrolu
        CalismaSaati mesai = calismaSaatiRepository.findByUzmanIdAndGunOfWeek(uzmanId, gunOfWeek)
                .orElse(null);

        // Tanımlı kayıt varsa ve kapalıysa boş slot dön
        if (mesai != null && Boolean.TRUE.equals(mesai.getKapaliMi())) {
            return new ArrayList<>();
        }

        // Hiç çalışma saati tanımlı değilse varsayılan kullan (09:00-18:00, Pazar kapalı)
        // Owner sonradan /calisanlar/[id] sayfasından özelleştirebilir.
        LocalTime baslangic;
        LocalTime bitis;
        if (mesai != null) {
            baslangic = mesai.getBaslangicSaat();
            bitis = mesai.getBitisSaat();
        } else {
            if (gunOfWeek == 7) return new ArrayList<>(); // Pazar default kapalı
            baslangic = LocalTime.of(9, 0);
            bitis = LocalTime.of(18, 0);
        }

        // Toplam sure ve buffer hesapla
        int toplamSure = 0;
        int toplamBufferOnce = 0;
        int toplamBufferSonra = 0;
        for (Long hizmetId : hizmetIds) {
            Hizmet hizmet = hizmetRepository.findById(hizmetId).orElse(null);
            if (hizmet == null || !Boolean.TRUE.equals(hizmet.getAktif())) continue;
            toplamSure += hizmet.getSureDakika();
            toplamBufferOnce = Math.max(toplamBufferOnce, hizmet.getBufferOnceDk());
            toplamBufferSonra = Math.max(toplamBufferSonra, hizmet.getBufferSonraDk());
        }

        int toplamBlokSure = toplamBufferOnce + toplamSure + toplamBufferSonra;

        // O gunun randevularini DB seviyesinde tarih araligi ile filtrele (heap'i sismeden)
        LocalDateTime gunBaslangic = tarih.atStartOfDay();
        LocalDateTime gunBitis = tarih.atTime(23, 59, 59);
        List<Randevu> oGunkuRandevular = randevuRepository
                .findByUzmanIdAndTarihSaatBetween(uzmanId, gunBaslangic, gunBitis).stream()
                .filter(r -> r.getDurum() != RandevuDurumu.IPTAL_EDILDI)
                .toList();

        List<LocalTime> bosSaatler = new ArrayList<>();
        // Bugun ise rezervasyon icin minimum 15dk buffer ekleyip ileriye yuvarla
        LocalTime suan = LocalTime.now().plusMinutes(15);
        LocalTime donguSaati = baslangic;

        while (!donguSaati.plusMinutes(toplamBlokSure).isAfter(bitis)) {
            // Bugunse gecmis + cok yakin saatleri atla
            if (tarih.equals(LocalDate.now()) && donguSaati.isBefore(suan)) {
                donguSaati = donguSaati.plusMinutes(15);
                continue;
            }

            LocalTime slotBaslangic = donguSaati.plusMinutes(toplamBufferOnce);
            LocalTime slotBitis = slotBaslangic.plusMinutes(toplamSure).plusMinutes(toplamBufferSonra);

            boolean cakismaVar = false;
            for (Randevu mevcut : oGunkuRandevular) {
                LocalTime rBaslangic = mevcut.getTarihSaat().toLocalTime();
                LocalTime rBitis = mevcut.getBitisTarihi().toLocalTime();

                if (donguSaati.isBefore(rBitis) && slotBitis.isAfter(rBaslangic)) {
                    cakismaVar = true;
                    break;
                }
            }

            if (!cakismaVar) {
                bosSaatler.add(slotBaslangic);
            }
            donguSaati = donguSaati.plusMinutes(15);
        }

        return bosSaatler;
    }

    public int calculateTotalDuration(List<Long> hizmetIds) {
        int total = 0;
        for (Long id : hizmetIds) {
            Hizmet h = hizmetRepository.findById(id).orElse(null);
            if (h != null && Boolean.TRUE.equals(h.getAktif())) total += h.getSureDakika();
        }
        return total;
    }

    public double calculateTotalPrice(List<Long> hizmetIds) {
        double total = 0;
        for (Long id : hizmetIds) {
            Hizmet h = hizmetRepository.findById(id).orElse(null);
            if (h != null && Boolean.TRUE.equals(h.getAktif())) total += h.getFiyat();
        }
        return total;
    }
}
