package com.appointflow.service;

import com.appointflow.dto.RandevuRequest;
import com.appointflow.dto.RandevuResponse;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RandevuService {

    private final RandevuRepository randevuRepository;
    private final KullaniciRepository kullaniciRepository;
    private final HizmetRepository hizmetRepository;
    private final CalismaSaatiRepository calismaSaatiRepository;

    public RandevuResponse randevuAl(RandevuRequest request, String musteriEmail) {
        Kullanici musteri = kullaniciRepository.findByEmail(musteriEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (musteri.getKaraListedeMi() != null && musteri.getKaraListedeMi()) {
            throw new RuntimeException("Hesabınız kara listede olduğu için randevu alamazsınız.");
        }

        Kullanici uzman = kullaniciRepository.findById(request.getUzmanId())
                .orElseThrow(() -> new RuntimeException("Uzman bulunamadı."));
        Hizmet hizmet = hizmetRepository.findById(request.getHizmetId())
                .orElseThrow(() -> new RuntimeException("Hizmet bulunamadı."));

        LocalDateTime baslangic = request.getTarihSaat();
        LocalDateTime bitis = baslangic.plusMinutes(hizmet.getSureDakika());

        int gunOfWeek = baslangic.getDayOfWeek().getValue();

        // 1. Uzmanin mesaisi açık mı?
        CalismaSaati mesai = calismaSaatiRepository.findByUzmanIdAndGunOfWeek(uzman.getId(), gunOfWeek)
                .orElseThrow(() -> new RuntimeException("Uzmanin bu gün için çalışma saati tanımlı değil."));

        if (mesai.getKapaliMi()) {
            throw new RuntimeException("Uzman seçilen günde kapalı.");
        }
        if (baslangic.toLocalTime().isBefore(mesai.getBaslangicSaat()) || bitis.toLocalTime().isAfter(mesai.getBitisSaat())) {
            throw new RuntimeException("Seçilen saatler uzmanin mesai saatleri (" + mesai.getBaslangicSaat() + " - " + mesai.getBitisSaat() + ") dışında.");
        }

        // 2. Randevu Çakışma Kontrolü (Uzmanin o günkü onaylanmış veya bekleyen randevularıyla)
        List<Randevu> uzmaninRandevulari = randevuRepository.findByUzmanId(uzman.getId());
        for (Randevu mevcut : uzmaninRandevulari) {
            if (mevcut.getDurum() != RandevuDurumu.IPTAL_EDILDI) {
                boolean cakismaVar = (baslangic.isBefore(mevcut.getBitisTarihi()) && bitis.isAfter(mevcut.getTarihSaat()));
                if (cakismaVar) {
                    throw new RuntimeException("Seçilen saat aralığı dolu. Uzmanin başka bir randevusu var.");
                }
            }
        }

        Randevu randevu = Randevu.builder()
                .musteri(musteri)
                .uzman(uzman)
                .hizmet(hizmet)
                .tarihSaat(baslangic)
                .bitisTarihi(bitis)
                .durum(RandevuDurumu.BEKLIYOR)
                .not(request.getNot())
                .build();

        randevuRepository.save(randevu);
        return toResponse(randevu);
    }

    public List<RandevuResponse> musteriRandevulari(String musteriEmail) {
        Kullanici musteri = kullaniciRepository.findByEmail(musteriEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        return randevuRepository.findByMusteriId(musteri.getId()).stream()
                .map(this::toResponse).toList();
    }

    public List<RandevuResponse> uzmanRandevulari(String uzmanEmail) {
        Kullanici uzman = kullaniciRepository.findByEmail(uzmanEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        return randevuRepository.findByUzmanId(uzman.getId()).stream()
                .map(this::toResponse).toList();
    }

    public RandevuResponse durumGuncelle(Long randevuId, RandevuDurumu yeniDurum, String email) {
        Randevu randevu = randevuRepository.findById(randevuId)
                .orElseThrow(() -> new RuntimeException("Randevu bulunamadı."));
        Kullanici kullanici = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        boolean uzmanMi = randevu.getUzman().getId().equals(kullanici.getId());
        boolean adminMi = kullanici.getRol() == Role.ADMIN || kullanici.getRol() == Role.OWNER;
        boolean musteriIptal = kullanici.getRol() == Role.MUSTERI
                && randevu.getMusteri().getId().equals(kullanici.getId())
                && yeniDurum == RandevuDurumu.IPTAL_EDILDI;

        if (!uzmanMi && !adminMi && !musteriIptal) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }

        // Kara liste ve sadakat kontrolü
        if (yeniDurum == RandevuDurumu.GELMEDI) {
            Kullanici musteri = randevu.getMusteri();
            musteri.setGelmemeSayisi(musteri.getGelmemeSayisi() + 1);
            if (musteri.getGelmemeSayisi() >= 3) {
                musteri.setKaraListedeMi(true);
            }
            kullaniciRepository.save(musteri);
        } else if (yeniDurum == RandevuDurumu.TAMAMLANDI) {
            Kullanici musteri = randevu.getMusteri();
            // Sadakat puanı ekle (Örneğin her hizmetin fiyatının %10'u kadar puan)
            Double kazanilanPuan = randevu.getHizmet().getFiyat() * 0.10;
            musteri.setSadakatPuani(musteri.getSadakatPuani() + kazanilanPuan.intValue());

            // Eğer müşteri sadakat puanını kullandıysa indirimi burada uyguluyoruz (Daha sonra eklenecek)
            // Şimdilik sadece tam fiyat yazıyoruz
            randevu.setOdenenTutar(randevu.getHizmet().getFiyat());
            kullaniciRepository.save(musteri);
        }

        randevu.setDurum(yeniDurum);
        randevuRepository.save(randevu);
        return toResponse(randevu);
    }

    public List<LocalTime> bosSaatleriGetir(Long uzmanId, Long hizmetId, LocalDate tarih) {
        Kullanici uzman = kullaniciRepository.findById(uzmanId)
                .orElseThrow(() -> new RuntimeException("Uzman bulunamadı."));
        Hizmet hizmet = hizmetRepository.findById(hizmetId)
                .orElseThrow(() -> new RuntimeException("Hizmet bulunamadı."));

        int gunOfWeek = tarih.getDayOfWeek().getValue();

        CalismaSaati mesai = calismaSaatiRepository.findByUzmanIdAndGunOfWeek(uzman.getId(), gunOfWeek)
                .orElse(null);

        if (mesai == null || mesai.getKapaliMi()) {
            return new ArrayList<>(); // Kapalı veya mesai tanımlı değilse boş liste döner
        }

        List<Randevu> oGunkuRandevular = randevuRepository.findByUzmanId(uzman.getId()).stream()
                .filter(r -> r.getTarihSaat().toLocalDate().equals(tarih))
                .filter(r -> r.getDurum() != RandevuDurumu.IPTAL_EDILDI)
                .toList();

        List<LocalTime> bosSaatler = new ArrayList<>();
        LocalTime suan = LocalTime.now();
        LocalTime donguSaati = mesai.getBaslangicSaat();

        while (donguSaati.plusMinutes(hizmet.getSureDakika()).isBefore(mesai.getBitisSaat()) ||
               donguSaati.plusMinutes(hizmet.getSureDakika()).equals(mesai.getBitisSaat())) {

            // Eğer tarih bugünse, geçmiş saatleri listeye ekleme
            if (tarih.equals(LocalDate.now()) && donguSaati.isBefore(suan)) {
                 donguSaati = donguSaati.plusMinutes(15);
                 continue;
            }

            LocalTime baslangic = donguSaati;
            LocalTime bitis = donguSaati.plusMinutes(hizmet.getSureDakika());

            boolean cakismaVar = false;
            for (Randevu mevcut : oGunkuRandevular) {
                LocalTime rBaslangic = mevcut.getTarihSaat().toLocalTime();
                LocalTime rBitis = mevcut.getBitisTarihi().toLocalTime();

                if (baslangic.isBefore(rBitis) && bitis.isAfter(rBaslangic)) {
                    cakismaVar = true;
                    break;
                }
            }

            if (!cakismaVar) {
                bosSaatler.add(baslangic);
            }
            // 15 dakikalık dilimler (slotlar) halinde ilerliyoruz. Dilenirse 30 da yapılabilir.
            donguSaati = donguSaati.plusMinutes(15);
        }

        return bosSaatler;
    }

    private RandevuResponse toResponse(Randevu r) {
        RandevuResponse res = new RandevuResponse();
        res.setId(r.getId());
        res.setMusteriAd(r.getMusteri().getAd() + " " + r.getMusteri().getSoyad());
        res.setUzmanAd(r.getUzman().getAd() + " " + r.getUzman().getSoyad());
        res.setHizmetAd(r.getHizmet().getAd());
        res.setFiyat(r.getHizmet().getFiyat());
        res.setSureDakika(r.getHizmet().getSureDakika());
        res.setTarihSaat(r.getTarihSaat());
        res.setDurum(r.getDurum());
        res.setNot(r.getNot());
        return res;
    }
}
