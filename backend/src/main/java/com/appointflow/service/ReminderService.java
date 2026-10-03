package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.AppointmentReminderRequest;
import com.appointflow.dto.AppointmentReminderResponse;
import com.appointflow.dto.ReminderSnoozeRequest;
import com.appointflow.dto.ReminderStatsResponse;
import com.appointflow.entity.*;
import com.appointflow.repository.*;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final AppointmentReminderRepository reminderRepository;
    private final RandevuRepository randevuRepository;
    private final CustomerRepository customerRepository;
    private final ReminderTemplateRepository templateRepository;
    private final WhatsappMessageService whatsappMessageService;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    private static final java.util.Set<String> ALLOWED_KANALLAR =
            java.util.Set.of("WHATSAPP", "SMS", "EMAIL");

    @Transactional
    public AppointmentReminderResponse createReminder(Long randevuId, AppointmentReminderRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Randevu randevu = randevuRepository.findById(randevuId)
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));
        if (!randevu.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }
        if (randevu.getCustomer() == null) {
            throw ApiException.badRequest("Bu randevuya ait musteri bulunamadi.");
        }

        ReminderTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId()).orElse(null);
        }

        String kanal = request.getKanal() != null ? request.getKanal().toUpperCase() : "WHATSAPP";
        if (!ALLOWED_KANALLAR.contains(kanal)) {
            throw ApiException.badRequest("Gecersiz kanal: " + kanal + ". Izinli: " + ALLOWED_KANALLAR);
        }

        AppointmentReminder reminder = AppointmentReminder.builder()
                .tenantId(tenantId)
                .randevu(randevu)
                .customer(randevu.getCustomer())
                .template(template)
                .mesaj(request.getMesaj())
                .kanal(kanal)
                .gonderimTarihi(request.getGonderimTarihi())
                .build();

        return toResponse(reminderRepository.save(reminder));
    }

    public List<AppointmentReminderResponse> getByRandevu(Long randevuId) {
        Long tenantId = TenantContext.getTenantId();
        Randevu randevu = randevuRepository.findById(randevuId)
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));
        if (!randevu.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }
        return reminderRepository.findByRandevuId(randevuId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentReminderResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return reminderRepository.findByTenantId(tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentReminderResponse> getPending() {
        Long tenantId = TenantContext.getTenantId();
        return reminderRepository.findByTenantIdAndStatus(tenantId, ReminderStatus.PENDING)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentReminderResponse snooze(Long reminderId, ReminderSnoozeRequest request) {
        Long tenantId = TenantContext.getTenantId();
        AppointmentReminder reminder = findByIdAndTenant(reminderId, tenantId);

        reminder.setStatus(ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(request.getYeniTarih());
        reminder.setSnoozeCount(reminder.getSnoozeCount() + 1);

        return toResponse(reminderRepository.save(reminder));
    }

    public ReminderStatsResponse getStats() {
        Long tenantId = TenantContext.getTenantId();
        return ReminderStatsResponse.builder()
                .toplam(reminderRepository.countByTenantId(tenantId))
                .beklemede(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.PENDING))
                .gonderildi(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.SENT))
                .yanitlandiEvet(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.RESPONDED_YES))
                .yanitlandiHayir(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.RESPONDED_NO))
                .ertelendi(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.SNOOZED))
                .iptal(reminderRepository.countByTenantIdAndStatus(tenantId, ReminderStatus.CANCELLED))
                .build();
    }

    @Transactional
    public AppointmentReminderResponse resend(Long reminderId) {
        Long tenantId = TenantContext.getTenantId();
        AppointmentReminder reminder = findByIdAndTenant(reminderId, tenantId);

        if (reminder.getCustomer() == null
                || reminder.getCustomer().getTelefon() == null
                || reminder.getCustomer().getTelefon().isBlank()) {
            throw ApiException.badRequest("Musterinin telefon bilgisi yok, hatirlatma gonderilemez.");
        }
        if (reminder.getCustomer().getKaraListedeMi()) {
            throw ApiException.badRequest("Musteri kara listede, hatirlatma gonderilemez.");
        }

        try {
            whatsappMessageService.sendTextMessage(
                    reminder.getTenantId(),
                    reminder.getCustomer().getTelefon(),
                    reminder.getMesaj());
        } catch (Exception e) {
            throw ApiException.badRequest("Hatirlatma gonderilemedi: " + e.getMessage());
        }

        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(LocalDateTime.now());
        reminder.setRespondedAt(null);
        return toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public AppointmentReminderResponse cancel(Long reminderId) {
        Long tenantId = TenantContext.getTenantId();
        AppointmentReminder reminder = findByIdAndTenant(reminderId, tenantId);
        reminder.setStatus(ReminderStatus.CANCELLED);
        return toResponse(reminderRepository.save(reminder));
    }

    // ─── Yanıt İşleme (WhatsApp webhook tarafından çağrılır) ──────────────────

    @Transactional
    public void handleResponse(Long customerId, String yanit) {
        // Müşterinin bekleyen (SENT) hatırlatmasını bul
        List<AppointmentReminder> sent = reminderRepository.findByCustomerId(customerId)
                .stream()
                .filter(r -> r.getStatus() == ReminderStatus.SENT)
                .collect(Collectors.toList());

        if (sent.isEmpty()) return;

        AppointmentReminder reminder = sent.get(0); // En son gönderileni işle
        reminder.setRespondedAt(LocalDateTime.now());

        String lower = yanit.toLowerCase().trim();

        if (lower.contains("evet") || lower.equals("e") || lower.equals("yes")) {
            reminder.setStatus(ReminderStatus.RESPONDED_YES);
            // AI randevu akışını başlatmak için conversation service burada tetiklenebilir
            // Şimdilik log — conversation entegrasyonu FAZ 4'te yapıldı
            log.info("Hatirlatma yaniti EVET: customerId={}, reminderId={}", customerId, reminder.getId());
        } else if (lower.contains("hayir") || lower.contains("hayır") || lower.equals("h") || lower.equals("no")) {
            reminder.setStatus(ReminderStatus.RESPONDED_NO);
            log.info("Hatirlatma yaniti HAYIR: customerId={}, reminderId={}", customerId, reminder.getId());
        } else {
            // Tarih içeriyorsa snooze (basit kontrol)
            reminder.setStatus(ReminderStatus.SNOOZED);
            reminder.setSnoozedUntil(LocalDate.now().plusWeeks(1)); // Varsayılan 1 hafta ertele
            reminder.setSnoozeCount(reminder.getSnoozeCount() + 1);
            log.info("Hatirlatma yaniti ERTELENDI: customerId={}, reminderId={}", customerId, reminder.getId());
        }

        reminderRepository.save(reminder);
    }

    // ─── Scheduled Job — Her gün 09:00 ────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDueReminders() {
        log.info("Hatirlatma job basliyor...");
        LocalDate bugun = LocalDate.now();

        List<AppointmentReminder> due = new ArrayList<>();
        due.addAll(reminderRepository.findDueReminders(bugun));
        due.addAll(reminderRepository.findDueSnoozedReminders(bugun));

        int sent = 0, failed = 0;

        for (AppointmentReminder reminder : due) {
            try {
                Customer customer = reminder.getCustomer();
                if (customer.getTelefon() == null || customer.getTelefon().isBlank()) continue;
                if (customer.getKaraListedeMi()) continue;

                whatsappMessageService.sendTextMessage(
                        reminder.getTenantId(),
                        customer.getTelefon(),
                        reminder.getMesaj());

                reminder.setStatus(ReminderStatus.SENT);
                reminder.setSentAt(LocalDateTime.now());
                reminderRepository.save(reminder);
                sent++;
            } catch (Exception e) {
                failed++;
                log.warn("Hatirlatma gonderilemedi: reminderId={}, hata={}", reminder.getId(), e.getMessage());
            }
        }

        log.info("Hatirlatma job tamamlandi: gonderilen={}, basarisiz={}", sent, failed);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private AppointmentReminder findByIdAndTenant(Long id, Long tenantId) {
        AppointmentReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Hatirlatma bulunamadi."));
        if (!reminder.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden("Bu hatirlatmaya erisim yetkiniz yok.");
        }
        return reminder;
    }

    private AppointmentReminderResponse toResponse(AppointmentReminder r) {
        Customer c = r.getCustomer();
        return AppointmentReminderResponse.builder()
                .id(r.getId())
                .randevuId(r.getRandevu().getId())
                .customerId(c.getId())
                .musteriAd(c.getAd() + " " + c.getSoyad())
                .musteriTelefon(c.getTelefon())
                .templateId(r.getTemplate() != null ? r.getTemplate().getId() : null)
                .mesaj(r.getMesaj())
                .kanal(r.getKanal())
                .gonderimTarihi(r.getGonderimTarihi())
                .status(r.getStatus().name())
                .sentAt(r.getSentAt())
                .respondedAt(r.getRespondedAt())
                .snoozedUntil(r.getSnoozedUntil())
                .snoozeCount(r.getSnoozeCount())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
