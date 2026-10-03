package com.appointflow.notification.dispatcher;

import com.appointflow.device.service.ExpoPushService;
import com.appointflow.entity.Kullanici;
import com.appointflow.notification.entity.Notification;
import com.appointflow.notification.service.NotificationService;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Producer noktalari (AppointmentService, BillingService, QuotaService, vb.) bu siniftan cagri yapar.
 * Yan etki: hem `notifications` tablosuna kayit, hem Expo Push'a tetik.
 * Hata olursa swallow edip log atilir — bildirim hatasi ana akisi bozmamali.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final ExpoPushService expoPushService;
    private final KullaniciRepository kullaniciRepository;

    /**
     * Tek kullaniciya bildirim gonderir.
     */
    @Transactional
    public void notifyUser(Long tenantId, Long userId, String tip, String baslik, String icerik, String link) {
        try {
            Notification n = notificationService.create(tenantId, userId, tip, baslik, icerik, link);
            expoPushService.pushToUser(userId, baslik, icerik, link);
            log.debug("Bildirim gonderildi — userId={}, tip={}, id={}", userId, tip, n.getId());
        } catch (Exception e) {
            log.warn("Bildirim gonderilemedi — userId={}, tip={}, hata={}", userId, tip, e.getMessage());
        }
    }

    /**
     * Tenant'in OWNER'ina bildirim gonderir (OWNER yoksa atlanir).
     */
    public void notifyOwner(Long tenantId, String tip, String baslik, String icerik, String link) {
        kullaniciRepository.findOwnerByTenantId(tenantId)
                .ifPresentOrElse(
                        owner -> notifyUser(tenantId, owner.getId(), tip, baslik, icerik, link),
                        () -> log.debug("OWNER bulunamadi, bildirim atlandi — tenantId={}", tenantId));
    }

    /**
     * Tenant'in OWNER + ADMIN'lerine bildirim gonderir.
     */
    public void notifyAdmins(Long tenantId, String tip, String baslik, String icerik, String link) {
        List<Kullanici> admins = kullaniciRepository.findAdminsByTenantId(tenantId);
        if (admins.isEmpty()) {
            log.debug("OWNER/ADMIN bulunamadi — tenantId={}", tenantId);
            return;
        }
        for (Kullanici a : admins) {
            notifyUser(tenantId, a.getId(), tip, baslik, icerik, link);
        }
    }

    /**
     * Notification tip sabitleri — frontend bu kelimelerle icon/renk seçer.
     */
    public static final class Tip {
        public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
        public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
        public static final String APPOINTMENT_COMPLETED = "APPOINTMENT_COMPLETED";
        public static final String QUOTA_WARNING = "QUOTA_WARNING";
        public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
        public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String SUBSCRIPTION_RENEWED = "SUBSCRIPTION_RENEWED";
        public static final String WHATSAPP_HANDOFF = "WHATSAPP_HANDOFF";

        private Tip() {}
    }
}
