package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.*;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final int SLOT_CAMPAIGN_MAX_RECIPIENTS = 50;
    private static final int SLOT_CAMPAIGN_EXPIRE_MINUTES = 30;

    /** Günde aynı segmente en fazla kaç kampanya gönderilebilir (spam koruması). */
    private static final int SEGMENT_CAMPAIGN_DAILY_LIMIT = 3;

    private final SlotCampaignRepository slotCampaignRepository;
    private final SlotCampaignMessageRepository slotCampaignMessageRepository;
    private final SegmentCampaignRepository segmentCampaignRepository;
    private final CustomerRepository customerRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final RandevuRepository randevuRepository;
    private final KullaniciRepository kullaniciRepository;
    private final WhatsappMessageService whatsappMessageService;

    // ─── Slot Campaign ─────────────────────────────────────────────────────────

    /**
     * Randevu iptal edildiğinde otomatik tetiklenir.
     * Sessizce çıkar, randevu akışını bozmaz.
     */
    @Transactional
    public void triggerSlotCampaign(Randevu cancelledRandevu) {
        Long tenantId = cancelledRandevu.getTenantId();

        // Saat kontrolü: 08:00 öncesi kampanya gönderme
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(8, 0))) {
            log.debug("Slot kampanya atlatildi — 08:00 oncesi: {}", now);
            return;
        }

        // 2 saat kala boş slot ise gönderme
        if (cancelledRandevu.getTarihSaat().isBefore(LocalDateTime.now().plusHours(2))) {
            log.debug("Slot kampanya atlatildi — slot 2 saatten az kaldi: {}", cancelledRandevu.getTarihSaat());
            return;
        }

        // Bu randevu için zaten kampanya var mı?
        if (slotCampaignRepository.findByTenantIdAndCancelledRandevuId(tenantId, cancelledRandevu.getId()).isPresent()) {
            return;
        }

        // Hedef müşteriler: son 30 günde gelen + aynı hizmeti alanlar (max 50)
        List<Customer> candidates = findSlotCandidates(tenantId, cancelledRandevu);
        if (candidates.isEmpty()) {
            log.debug("Slot kampanya icin aday musteri bulunamadi — randevuId={}", cancelledRandevu.getId());
            return;
        }

        String mesaj = buildSlotMessage(cancelledRandevu);

        SlotCampaign campaign = SlotCampaign.builder()
                .tenantId(tenantId)
                .cancelledRandevu(cancelledRandevu)
                .slotTime(cancelledRandevu.getTarihSaat())
                .staffId(cancelledRandevu.getUzman().getId())
                .mesaj(mesaj)
                .expiresAt(LocalDateTime.now().plusMinutes(SLOT_CAMPAIGN_EXPIRE_MINUTES))
                .build();
        slotCampaignRepository.save(campaign);

        int sent = 0;
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        for (Customer customer : candidates) {
            if (sent >= SLOT_CAMPAIGN_MAX_RECIPIENTS) break;

            // Spam koruması: bugün bu müşteriye zaten kampanya gönderilmiş mi?
            if (slotCampaignMessageRepository.existsByCustomerIdSince(customer.getId(), todayStart)) {
                continue;
            }

            // Telefon numarası yoksa atla
            if (customer.getTelefon() == null || customer.getTelefon().isBlank()) continue;

            try {
                whatsappMessageService.sendTextMessage(tenantId, customer.getTelefon(), mesaj);

                SlotCampaignMessage msg = SlotCampaignMessage.builder()
                        .campaign(campaign)
                        .customer(customer)
                        .durum("SENT")
                        .build();
                slotCampaignMessageRepository.save(msg);
                sent++;
            } catch (Exception e) {
                log.warn("Slot kampanya mesaji gonderilemedi: customerId={}, hata={}", customer.getId(), e.getMessage());
            }
        }

        campaign.setSentCount(sent);
        slotCampaignRepository.save(campaign);
        log.info("Slot kampanya olusturuldu: campaignId={}, gonderilen={}", campaign.getId(), sent);
    }

    public List<SlotCampaignResponse> getSlotCampaigns() {
        Long tenantId = TenantContext.getTenantId();
        return slotCampaignRepository.findByTenantId(tenantId)
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    @Transactional
    public SlotCampaignResponse cancelSlotCampaign(Long id) {
        Long tenantId = TenantContext.getTenantId();
        SlotCampaign campaign = slotCampaignRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Kampanya bulunamadi."));
        if (!campaign.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu kampanyaya erisim yetkiniz yok.");
        }
        campaign.setStatus(CampaignStatus.CANCELLED);
        return toSlotResponse(slotCampaignRepository.save(campaign));
    }

    // ─── Segment Campaign ──────────────────────────────────────────────────────

    @Transactional
    public SegmentCampaignResponse sendSegmentCampaign(SegmentCampaignRequest request, String creatorEmail) {
        Long tenantId = TenantContext.getTenantId();

        // Segment tipini doğrula
        SegmentType segmentType = parseSegmentType(request.getHedefSegment());

        // Saat kontrolü: 08:00 öncesi gönderme
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(8, 0))) {
            throw ApiException.badRequest("Kampanya 08:00 oncesinde gonderilemez.");
        }

        // Spam koruması: bu segmente bugün gönderilen kampanya sayısı limiti aşmasın
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayCount = segmentCampaignRepository.countByTenantIdAndSegmentSince(tenantId, segmentType, todayStart);
        if (todayCount >= SEGMENT_CAMPAIGN_DAILY_LIMIT) {
            throw ApiException.badRequest(
                    "Bu segmente bugün " + SEGMENT_CAMPAIGN_DAILY_LIMIT + " kampanya gönderildi (günlük limit). Yarın tekrar deneyin.");
        }

        Long creatorId = kullaniciRepository.findByEmail(creatorEmail)
                .map(k -> k.getId())
                .orElse(null);

        SegmentCampaign campaign = SegmentCampaign.builder()
                .tenantId(tenantId)
                .baslik(request.getBaslik())
                .hedefSegment(segmentType)
                .mesaj(request.getMesaj())
                .createdById(creatorId)
                .build();
        segmentCampaignRepository.save(campaign);

        // Hedef segmentteki müşterileri bul
        List<CustomerSegment> segments = customerSegmentRepository
                .findByTenantIdAndSegmentType(tenantId, segmentType);

        int sent = 0;
        LocalDateTime todayStartForSpam = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        for (CustomerSegment seg : segments) {
            Customer customer = seg.getCustomer();
            if (customer.getTelefon() == null || customer.getTelefon().isBlank()) continue;
            if (customer.getKaraListedeMi()) continue;

            // Slot kampanya spam kontrolü — bugün bu müşteriye zaten mesaj gitti mi?
            if (slotCampaignMessageRepository.existsByCustomerIdSince(customer.getId(), todayStartForSpam)) {
                continue;
            }

            try {
                String personalizedMesaj = applyPlaceholders(request.getMesaj(), customer);
                whatsappMessageService.sendTextMessage(tenantId, customer.getTelefon(), personalizedMesaj);
                sent++;
            } catch (Exception e) {
                log.warn("Segment kampanya mesaji gonderilemedi: customerId={}, hata={}", customer.getId(), e.getMessage());
            }
        }

        campaign.setSentCount(sent);
        campaign.setStatus(CampaignStatus.COMPLETED);
        segmentCampaignRepository.save(campaign);

        log.info("Segment kampanya tamamlandi: campaignId={}, segment={}, gonderilen={}",
                campaign.getId(), segmentType, sent);
        return toSegmentResponse(campaign);
    }

    public List<SegmentCampaignResponse> getSegmentCampaigns() {
        Long tenantId = TenantContext.getTenantId();
        return segmentCampaignRepository.findByTenantId(tenantId)
                .stream().map(this::toSegmentResponse).collect(Collectors.toList());
    }

    // ─── Scheduled: Expire old slot campaigns ─────────────────────────────────

    @Scheduled(fixedDelay = 60_000) // Her dakika kontrol
    @Transactional
    public void expireSlotCampaigns() {
        List<SlotCampaign> expired = slotCampaignRepository
                .findByStatusAndExpiresAtBefore(CampaignStatus.ACTIVE, LocalDateTime.now());
        for (SlotCampaign c : expired) {
            c.setStatus(CampaignStatus.EXPIRED);
            slotCampaignRepository.save(c);
            log.debug("Slot kampanya suresi doldu: campaignId={}", c.getId());
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private List<Customer> findSlotCandidates(Long tenantId, Randevu cancelledRandevu) {
        Long hizmetId = cancelledRandevu.getHizmet() != null ? cancelledRandevu.getHizmet().getId() : null;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Son 30 günde gelen tüm müşteriler
        List<Customer> recent = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDurum() == RandevuDurumu.TAMAMLANDI)
                .filter(r -> r.getTarihSaat().isAfter(thirtyDaysAgo))
                .filter(r -> r.getCustomer() != null)
                .filter(r -> !r.getCustomer().getKaraListedeMi())
                .map(Randevu::getCustomer)
                .distinct()
                .collect(Collectors.toList());

        if (hizmetId == null) return recent.stream().limit(SLOT_CAMPAIGN_MAX_RECIPIENTS).collect(Collectors.toList());

        // Aynı hizmeti alanlar önce, sonra diğerleri
        Long finalHizmetId = hizmetId;
        List<Customer> sameService = recent.stream()
                .filter(c -> randevuRepository.findByTenantIdAndCustomerId(tenantId, c.getId()).stream()
                        .anyMatch(r -> r.getHizmet() != null && r.getHizmet().getId().equals(finalHizmetId)))
                .collect(Collectors.toList());

        List<Customer> others = recent.stream()
                .filter(c -> !sameService.contains(c))
                .collect(Collectors.toList());

        sameService.addAll(others);
        return sameService.stream().limit(SLOT_CAMPAIGN_MAX_RECIPIENTS).collect(Collectors.toList());
    }

    private String buildSlotMessage(Randevu randevu) {
        String tarih = randevu.getTarihSaat().toLocalDate().toString();
        String saat = randevu.getTarihSaat().toLocalTime().toString();
        String uzman = randevu.getUzman().getAd() + " " + randevu.getUzman().getSoyad();
        return String.format(
                "Merhaba! %s tarihinde %s saatinde %s icin bir slot acildi. "
                + "Randevu almak ister misiniz? Evet yazmaniz yeterli 🙂",
                tarih, saat, uzman);
    }

    /**
     * Mesaj sablonundaki placeholder'lari musteri verisiyle doldur.
     * Desteklenen: {ad}, {soyad}, {telefon}.
     * Randevu bazli ({tarih}, {saat}, {hizmet}) slot kampanyada uygulanir.
     */
    private String applyPlaceholders(String template, Customer customer) {
        if (template == null || template.isEmpty() || customer == null) return template;
        String ad = customer.getAd() != null ? customer.getAd() : "";
        String soyad = customer.getSoyad() != null ? customer.getSoyad() : "";
        String telefon = customer.getTelefon() != null ? customer.getTelefon() : "";
        return template
                .replace("{ad}", ad)
                .replace("{soyad}", soyad)
                .replace("{telefon}", telefon);
    }

    private SegmentType parseSegmentType(String type) {
        try {
            return SegmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz segment tipi: " + type);
        }
    }

    private SlotCampaignResponse toSlotResponse(SlotCampaign c) {
        return SlotCampaignResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .cancelledRandevuId(c.getCancelledRandevu().getId())
                .slotTime(c.getSlotTime())
                .staffId(c.getStaffId())
                .mesaj(c.getMesaj())
                .sentCount(c.getSentCount())
                .status(c.getStatus().name())
                .filledRandevuId(c.getFilledRandevu() != null ? c.getFilledRandevu().getId() : null)
                .expiresAt(c.getExpiresAt())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private SegmentCampaignResponse toSegmentResponse(SegmentCampaign c) {
        double conversionRate = c.getSentCount() > 0
                ? (double) c.getResponseCount() / c.getSentCount() * 100
                : 0.0;
        return SegmentCampaignResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .baslik(c.getBaslik())
                .hedefSegment(c.getHedefSegment().name())
                .mesaj(c.getMesaj())
                .sentCount(c.getSentCount())
                .responseCount(c.getResponseCount())
                .conversionRate(Math.round(conversionRate * 10.0) / 10.0)
                .createdById(c.getCreatedById())
                .status(c.getStatus().name())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
