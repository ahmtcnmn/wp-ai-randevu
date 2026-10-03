package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.*;
import com.appointflow.entity.*;
import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.repository.*;
import com.appointflow.subscription.service.QuotaService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final RandevuRepository randevuRepository;
    private final KullaniciRepository kullaniciRepository;
    private final CustomerRepository customerRepository;
    private final HizmetRepository hizmetRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final AppointmentNoteRepository appointmentNoteRepository;
    private final com.appointflow.repository.ProductSaleRepository productSaleRepository;
    private final SlotCalculationService slotCalculationService;
    private final CommissionService commissionService;
    private final CampaignService campaignService;
    private final QuotaService quotaService;
    private final NotificationDispatcher notificationDispatcher;
    private final com.appointflow.audit.service.AuditService auditService;

    public List<AppointmentResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return randevuRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AppointmentResponse getById(Long id) {
        return toResponse(findByIdAndTenant(id));
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Long tenantId = TenantContext.getTenantId();

        quotaService.assertAppointmentQuota(tenantId);

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> ApiException.notFound("Musteri bulunamadi."));
        if (!tenantId.equals(customer.getTenantId())) {
            throw ApiException.notFound("Musteri bulunamadi.");
        }
        if (customer.getKaraListedeMi()) {
            throw ApiException.badRequest("Musteri kara listede, randevu olusturulamaz.");
        }

        Kullanici uzman = kullaniciRepository.findById(request.getUzmanId())
                .orElseThrow(() -> ApiException.notFound("Uzman bulunamadi."));
        if (!tenantId.equals(uzman.getTenantId())) {
            throw ApiException.notFound("Uzman bulunamadi.");
        }
        if (!Boolean.TRUE.equals(uzman.getAktif())) {
            throw ApiException.badRequest("Secilen uzman aktif degil.");
        }

        if (request.getTarihSaat().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Gecmis tarihe randevu olusturulamaz.");
        }

        // Hizmetleri dogrula
        if (request.getHizmetIds() == null || request.getHizmetIds().isEmpty()) {
            throw ApiException.badRequest("En az bir hizmet seçilmelidir.");
        }
        List<Hizmet> hizmetler = new ArrayList<>();
        for (Long hizmetId : request.getHizmetIds()) {
            if (hizmetId == null) {
                throw ApiException.badRequest("Hizmet ID değeri boş (null) olamaz.");
            }
            Hizmet h = hizmetRepository.findById(hizmetId)
                    .orElseThrow(() -> ApiException.notFound("Hizmet bulunamadı: " + hizmetId));
            if (!tenantId.equals(h.getTenantId())) {
                throw ApiException.notFound("Hizmet bulunamadı: " + hizmetId);
            }
            if (!Boolean.TRUE.equals(h.getAktif())) {
                throw ApiException.badRequest("Hizmet aktif değil: " + h.getAd());
            }
            hizmetler.add(h);
        }

        int toplamSure = slotCalculationService.calculateTotalDuration(request.getHizmetIds());
        double toplamFiyat = slotCalculationService.calculateTotalPrice(request.getHizmetIds());

        LocalDateTime baslangic = request.getTarihSaat();
        LocalDateTime bitis = baslangic.plusMinutes(toplamSure);

        // Slot musaitlik kontrolu
        List<LocalTime> availableSlots = slotCalculationService.getAvailableSlots(
                request.getUzmanId(), request.getHizmetIds(), baslangic.toLocalDate());
        if (!availableSlots.contains(baslangic.toLocalTime())) {
            throw ApiException.badRequest("Secilen saat musait degil.");
        }

        RandevuKaynak kaynak = RandevuKaynak.MANUAL;
        if (request.getKaynak() != null) {
            try {
                kaynak = RandevuKaynak.valueOf(request.getKaynak().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Randevu randevu = Randevu.builder()
                .tenantId(tenantId)
                .customer(customer)
                .uzman(uzman)
                .hizmet(hizmetler.get(0)) // Ana hizmet (geriye uyumluluk)
                .tarihSaat(baslangic)
                .bitisTarihi(bitis)
                .durum(RandevuDurumu.BEKLIYOR)
                .kaynak(kaynak)
                .toplamFiyat(toplamFiyat)
                .toplamSureDk(toplamSure)
                .not(request.getNot())
                .build();
        randevuRepository.save(randevu);
        quotaService.incrementAppointmentCounter(tenantId);

        // Coklu hizmet kaydi (fiyat snapshot)
        for (Hizmet h : hizmetler) {
            AppointmentServiceEntity ase = AppointmentServiceEntity.builder()
                    .randevu(randevu)
                    .hizmet(h)
                    .fiyatSnapshot(h.getFiyat())
                    .sureDk(h.getSureDakika())
                    .build();
            appointmentServiceRepository.save(ase);
        }

        // Bildirim: yeni randevu — OWNER/ADMIN'lere + atanan uzmana
        String musteriAdSoyad = customer.getAd() + " " + customer.getSoyad();
        String ilkHizmet = hizmetler.get(0).getAd();
        String baslik = "Yeni randevu";
        String icerik = String.format("%s — %s, %s",
                musteriAdSoyad, ilkHizmet, baslangic.toString());
        String link = "/randevular/" + randevu.getId();
        notificationDispatcher.notifyAdmins(tenantId, NotificationDispatcher.Tip.APPOINTMENT_CREATED, baslik, icerik, link);
        if (!uzman.getRol().name().equals("OWNER") && !uzman.getRol().name().equals("ADMIN")) {
            notificationDispatcher.notifyUser(tenantId, uzman.getId(),
                    NotificationDispatcher.Tip.APPOINTMENT_CREATED, baslik, icerik, link);
        }

        auditService.log("APPOINTMENT_CREATE", "Randevu", randevu.getId(),
                java.util.Map.of("customerId", customer.getId(), "uzmanId", uzman.getId(),
                        "tarihSaat", baslangic.toString(), "kaynak", kaynak.name()));

        return toResponse(randevu);
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Randevu randevu = findByIdAndTenant(id);

        if (randevu.getDurum() == RandevuDurumu.TAMAMLANDI
                || randevu.getDurum() == RandevuDurumu.IPTAL_EDILDI
                || randevu.getDurum() == RandevuDurumu.GELMEDI) {
            throw ApiException.badRequest("Bu randevu degistirilemez (durum: " + randevu.getDurum() + ").");
        }

        if (request.getTarihSaat().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Gecmis tarihe randevu guncellenemez.");
        }

        if (request.getHizmetIds() == null || request.getHizmetIds().isEmpty()) {
            throw ApiException.badRequest("En az bir hizmet secilmelidir.");
        }

        List<Hizmet> hizmetler = new ArrayList<>();
        for (Long hizmetId : request.getHizmetIds()) {
            if (hizmetId == null) {
                throw ApiException.badRequest("Hizmet ID değeri boş (null) olamaz.");
            }
            Hizmet h = hizmetRepository.findById(hizmetId)
                    .orElseThrow(() -> ApiException.notFound("Hizmet bulunamadı: " + hizmetId));
            if (!tenantId.equals(h.getTenantId())) {
                throw ApiException.notFound("Hizmet bulunamadı: " + hizmetId);
            }
            if (!Boolean.TRUE.equals(h.getAktif())) {
                throw ApiException.badRequest("Hizmet aktif değil: " + h.getAd());
            }
            hizmetler.add(h);
        }

        int toplamSure = slotCalculationService.calculateTotalDuration(request.getHizmetIds());
        double toplamFiyat = slotCalculationService.calculateTotalPrice(request.getHizmetIds());

        LocalDateTime baslangic = request.getTarihSaat();
        LocalDateTime bitis = baslangic.plusMinutes(toplamSure);

        // Slot musaitlik kontrolu — randevunun kendi slot'u haric musait olmali
        List<LocalTime> availableSlots = slotCalculationService.getAvailableSlots(
                randevu.getUzman().getId(), request.getHizmetIds(), baslangic.toLocalDate());
        boolean sameStart = randevu.getTarihSaat().toLocalDate().equals(baslangic.toLocalDate())
                && randevu.getTarihSaat().toLocalTime().equals(baslangic.toLocalTime());
        if (!sameStart && !availableSlots.contains(baslangic.toLocalTime())) {
            throw ApiException.badRequest("Secilen saat musait degil.");
        }

        randevu.setTarihSaat(baslangic);
        randevu.setBitisTarihi(bitis);
        randevu.setHizmet(hizmetler.get(0));
        randevu.setToplamFiyat(toplamFiyat);
        randevu.setToplamSureDk(toplamSure);

        try {
            randevuRepository.saveAndFlush(randevu);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw ApiException.badRequest("Randevu bu sirada baska bir istek tarafindan guncellendi. Lutfen tekrar deneyin.");
        }

        // Eski coklu hizmet kayitlarini sil, yenilerini olustur (fiyat snapshot)
        List<AppointmentServiceEntity> mevcut = appointmentServiceRepository.findByRandevuId(randevu.getId());
        appointmentServiceRepository.deleteAll(mevcut);
        for (Hizmet h : hizmetler) {
            AppointmentServiceEntity ase = AppointmentServiceEntity.builder()
                    .randevu(randevu)
                    .hizmet(h)
                    .fiyatSnapshot(h.getFiyat())
                    .sureDk(h.getSureDakika())
                    .build();
            appointmentServiceRepository.save(ase);
        }

        return toResponse(randevu);
    }

    public AppointmentResponse updateStatus(Long id, String durum, java.math.BigDecimal toplamFiyat) {
        if (durum == null || durum.isBlank()) {
            throw ApiException.badRequest("durum alani zorunlu.");
        }
        if (toplamFiyat != null && toplamFiyat.signum() < 0) {
            throw ApiException.badRequest("Toplam fiyat negatif olamaz.");
        }
        Randevu randevu = findByIdAndTenant(id);
        RandevuDurumu mevcutDurum = randevu.getDurum();

        RandevuDurumu yeniDurum;
        try {
            yeniDurum = RandevuDurumu.valueOf(durum.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz durum: " + durum);
        }

        // State machine — terminal durumlardan cikis yok
        if (mevcutDurum == RandevuDurumu.TAMAMLANDI
                || mevcutDurum == RandevuDurumu.IPTAL_EDILDI
                || mevcutDurum == RandevuDurumu.GELMEDI) {
            if (mevcutDurum != yeniDurum) {
                throw ApiException.badRequest(
                        "Randevu durumu '" + mevcutDurum + "' — terminal durum degistirilemez.");
            }
        }

        randevu.setDurum(yeniDurum);

        // toplamFiyat request'ten geldiyse güncelle
        if (toplamFiyat != null) {
            randevu.setToplamFiyat(toplamFiyat.doubleValue());
        }

        if (yeniDurum == RandevuDurumu.TAMAMLANDI && randevu.getCustomer() != null) {
            Customer c = randevu.getCustomer();
            c.setSonZiyaret(LocalDateTime.now());
            Double puan = randevu.getToplamFiyat() != null ? randevu.getToplamFiyat() * 0.10 : 0;
            c.setSadakatPuani(c.getSadakatPuani() + puan.intValue());
            customerRepository.save(c);
            randevu.setOdenenTutar(randevu.getToplamFiyat());
        }

        if (yeniDurum == RandevuDurumu.TAMAMLANDI) {
            try {
                commissionService.createEarningForAppointment(randevu);
            } catch (Exception e) {
                log.warn("Earning olusturulamadi: randevuId={}, hata={}", randevu.getId(), e.getMessage());
            }
        }

        if (yeniDurum == RandevuDurumu.GELMEDI && randevu.getCustomer() != null) {
            Customer c = randevu.getCustomer();
            c.setGelmemeSayisi(c.getGelmemeSayisi() + 1);
            if (c.getGelmemeSayisi() >= 3) {
                c.setKaraListedeMi(true);
            }
            customerRepository.save(c);
        }

        randevuRepository.save(randevu);
        auditService.log("APPOINTMENT_STATUS_CHANGE", "Randevu", randevu.getId(),
                java.util.Map.of("eskiDurum", mevcutDurum.name(), "yeniDurum", yeniDurum.name(),
                        "toplamFiyat", toplamFiyat != null ? toplamFiyat.toString() : "-"));
        return toResponse(randevu);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, String neden) {
        Randevu randevu = findByIdAndTenant(id);
        if (randevu.getDurum() == RandevuDurumu.IPTAL_EDILDI) {
            throw ApiException.badRequest("Randevu zaten iptal edilmis.");
        }
        if (randevu.getDurum() == RandevuDurumu.TAMAMLANDI) {
            throw ApiException.badRequest("Tamamlanmis randevu iptal edilemez.");
        }
        randevu.setDurum(RandevuDurumu.IPTAL_EDILDI);
        randevu.setIptalNedeni(neden);
        try {
            randevuRepository.saveAndFlush(randevu);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw ApiException.badRequest("Randevu bu sirada baska bir istek tarafindan guncellendi. Lutfen tekrar deneyin.");
        }

        // Iptal edilen randevu o ayin kotasindan dusulur — counter olusturulma ayina baglidir
        // (QuotaService.appointmentKey YearMonth.now() kullanir, increment create aninda olur)
        if (randevu.getCreatedAt() != null
                && java.time.YearMonth.from(randevu.getCreatedAt()).equals(java.time.YearMonth.now())) {
            try {
                quotaService.decrementAppointmentCounter(randevu.getTenantId());
            } catch (Exception e) {
                log.warn("Quota decrement basarisiz: randevuId={}, hata={}", randevu.getId(), e.getMessage());
            }
        }

        try {
            campaignService.triggerSlotCampaign(randevu);
        } catch (Exception e) {
            log.warn("Slot kampanya tetiklenemedi: randevuId={}, hata={}", randevu.getId(), e.getMessage());
        }

        // Bildirim: iptal — OWNER/ADMIN'lere + atanan uzmana
        String musteriAdSoyad = randevu.getCustomer() != null
                ? randevu.getCustomer().getAd() + " " + randevu.getCustomer().getSoyad()
                : "Bilinmeyen musteri";
        String baslik = "Randevu iptal edildi";
        String icerik = String.format("%s — %s%s",
                musteriAdSoyad,
                randevu.getTarihSaat() != null ? randevu.getTarihSaat().toString() : "",
                neden != null && !neden.isBlank() ? " (" + neden + ")" : "");
        String link = "/randevular/" + randevu.getId();
        notificationDispatcher.notifyAdmins(randevu.getTenantId(),
                NotificationDispatcher.Tip.APPOINTMENT_CANCELLED, baslik, icerik, link);
        if (randevu.getUzman() != null) {
            String rol = randevu.getUzman().getRol().name();
            if (!rol.equals("OWNER") && !rol.equals("ADMIN")) {
                notificationDispatcher.notifyUser(randevu.getTenantId(), randevu.getUzman().getId(),
                        NotificationDispatcher.Tip.APPOINTMENT_CANCELLED, baslik, icerik, link);
            }
        }

        auditService.log("APPOINTMENT_CANCEL", "Randevu", randevu.getId(),
                neden != null ? java.util.Map.of("neden", neden) : null);

        return toResponse(randevu);
    }

    private static final java.util.Set<String> ALLOWED_NOTE_TURLERI =
            java.util.Set.of("INTERNAL", "MUSTERI", "REMINDER");

    public void addNote(Long randevuId, AppointmentNoteRequest request, String email) {
        Randevu randevu = findByIdAndTenant(randevuId);
        Kullanici yazan = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        String tur = request.getTur() != null ? request.getTur().toUpperCase() : "INTERNAL";
        if (!ALLOWED_NOTE_TURLERI.contains(tur)) {
            throw ApiException.badRequest("Gecersiz not turu: " + tur + ". Izinli: " + ALLOWED_NOTE_TURLERI);
        }

        AppointmentNote note = AppointmentNote.builder()
                .randevu(randevu)
                .yazan(yazan)
                .icerik(request.getIcerik())
                .tur(tur)
                .build();
        appointmentNoteRepository.save(note);
    }

    public List<LocalTime> getAvailability(Long uzmanId, List<Long> hizmetIds, java.time.LocalDate tarih) {
        return slotCalculationService.getAvailableSlots(uzmanId, hizmetIds, tarih);
    }

    public List<UserResponse> getStaffByTenant(Long tenantId) {
        return kullaniciRepository.findByTenantId(tenantId).stream()
                .filter(k -> k.getRol() == com.appointflow.entity.Role.STAFF
                          || k.getRol() == com.appointflow.entity.Role.BRANCH_MANAGER)
                .filter(k -> Boolean.TRUE.equals(k.getAktif()))
                .map(k -> UserResponse.builder()
                        .id(k.getId())
                        .ad(k.getAd())
                        .soyad(k.getSoyad())
                        .email(k.getEmail())
                        .telefon(k.getTelefon())
                        .rol(k.getRol().name())
                        .subeId(k.getSube() != null ? k.getSube().getId() : null)
                        .subeAd(k.getSube() != null ? k.getSube().getAd() : null)
                        .aktif(k.getAktif())
                        .build())
                .collect(Collectors.toList());
    }

    private Randevu findByIdAndTenant(Long id) {
        Randevu r = randevuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));
        if (!r.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }
        return r;
    }

    private AppointmentResponse toResponse(Randevu r) {
        String musteriAd = "";
        Long customerId = null;
        if (r.getCustomer() != null) {
            musteriAd = r.getCustomer().getAd() + " " + r.getCustomer().getSoyad();
            customerId = r.getCustomer().getId();
        } else if (r.getMusteri() != null) {
            musteriAd = r.getMusteri().getAd() + " " + r.getMusteri().getSoyad();
        }

        List<AppointmentServiceEntity> serviceEntities = appointmentServiceRepository.findByRandevuId(r.getId());
        List<AppointmentResponse.AppointmentServiceItem> items = serviceEntities.stream()
                .map(ase -> AppointmentResponse.AppointmentServiceItem.builder()
                        .hizmetId(ase.getHizmet().getId())
                        .hizmetAd(ase.getHizmet().getAd())
                        .fiyat(ase.getFiyatSnapshot())
                        .sureDk(ase.getSureDk())
                        .build())
                .collect(Collectors.toList());

        // Eger coklu hizmet kaydı yoksa (eski randevular), ana hizmetten olustur
        if (items.isEmpty() && r.getHizmet() != null) {
            items.add(AppointmentResponse.AppointmentServiceItem.builder()
                    .hizmetId(r.getHizmet().getId())
                    .hizmetAd(r.getHizmet().getAd())
                    .fiyat(r.getHizmet().getFiyat())
                    .sureDk(r.getHizmet().getSureDakika())
                    .build());
        }

        // Ürün satışlarını topla
        double urunToplami = productSaleRepository.findByRandevuId(r.getId()).stream()
                .mapToDouble(s -> s.getToplamTutar() != null ? s.getToplamTutar().doubleValue() : 0.0)
                .sum();
        double hizmetToplami = r.getToplamFiyat() != null ? r.getToplamFiyat() : 0.0;
        double genelToplam = hizmetToplami + urunToplami;

        return AppointmentResponse.builder()
                .id(r.getId())
                .musteriAd(musteriAd)
                .customerId(customerId)
                .uzmanAd(r.getUzman().getAd() + " " + r.getUzman().getSoyad())
                .uzmanId(r.getUzman().getId())
                .hizmetler(items)
                .tarihSaat(r.getTarihSaat())
                .bitisTarihi(r.getBitisTarihi())
                .durum(r.getDurum().name())
                .kaynak(r.getKaynak() != null ? r.getKaynak().name() : "MANUAL")
                .toplamFiyat(hizmetToplami)
                .urunToplami(urunToplami)
                .genelToplam(genelToplam)
                .odenenTutar(r.getOdenenTutar())
                .toplamSureDk(r.getToplamSureDk())
                .not(r.getNot())
                .iptalNedeni(r.getIptalNedeni())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
