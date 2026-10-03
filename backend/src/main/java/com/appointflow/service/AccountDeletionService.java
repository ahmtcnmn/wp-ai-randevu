package com.appointflow.service;

import com.appointflow.audit.service.AuditService;
import com.appointflow.common.ApiException;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.TenantRepository;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.SubscriptionRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Hesap silme — soft delete kalıbı.
 * Owner tetikler → tenant + tüm kullanıcıları için deletedAt set edilir.
 * Aktif subscription varsa İptal Edilir (CANCELLED). 30 gün geri alma penceresi.
 * AccountHardDeleteJob bu süre sonunda gerçek silme yapar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final TenantRepository tenantRepository;
    private final KullaniciRepository kullaniciRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditService auditService;

    @Transactional
    public void requestAccountDeletion(String requesterEmail) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw ApiException.badRequest("Tenant bulunamadi.");

        Kullanici requester = kullaniciRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        if (requester.getRol() != Role.OWNER) {
            throw ApiException.forbidden("Yalnizca isletme sahibi hesap silebilir.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
        if (tenant.getDeletedAt() != null) {
            throw ApiException.badRequest("Hesap zaten silinmek uzere isaretli.");
        }

        LocalDateTime now = LocalDateTime.now();
        long timestamp = now.toEpochSecond(java.time.ZoneOffset.UTC);

        // Tenant slug + email "deleted" işareti — aynı slug/email ile yeniden kayıt mümkün olsun
        tenant.setDeletedAt(now);
        tenant.setAktif(false);
        if (tenant.getEmail() != null && !tenant.getEmail().contains(".deleted.")) {
            tenant.setEmail(tenant.getEmail() + ".deleted." + timestamp);
        }
        if (tenant.getSlug() != null && !tenant.getSlug().contains(".deleted.")) {
            tenant.setSlug(tenant.getSlug() + ".deleted." + timestamp);
        }
        tenantRepository.save(tenant);

        // Kullanıcı email'lerini de rename et — aynı mail ile yeniden register edilebilsin
        List<Kullanici> users = kullaniciRepository.findByTenantId(tenantId);
        for (Kullanici k : users) {
            k.setDeletedAt(now);
            k.setAktif(false);
            if (k.getEmail() != null && !k.getEmail().contains(".deleted.")) {
                k.setEmail(k.getEmail() + ".deleted." + timestamp);
            }
        }
        kullaniciRepository.saveAll(users);

        // Aktif subscription iptal et — billing duracak ama veri 30 gün korunacak
        subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.TRIALING)
                .ifPresent(s -> {
                    s.setStatus(SubscriptionStatus.CANCELLED);
                    s.setIptalTarihi(now);
                    subscriptionRepository.save(s);
                });

        auditService.log("ACCOUNT_DELETE_REQUEST", "Tenant", tenantId,
                Map.of("requester", requesterEmail, "scheduledHardDeleteAfterDays", 30));
        log.warn("Hesap silme talebi: tenantId={}, requester={}, hard delete 30 gun sonra", tenantId, requesterEmail);
    }

    @Transactional
    public void restoreAccount(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
        if (tenant.getDeletedAt() == null) {
            throw ApiException.badRequest("Hesap zaten aktif.");
        }
        tenant.setDeletedAt(null);
        tenant.setAktif(true);
        // .deleted.{ts} eki varsa kaldır — eski email/slug geri gelsin
        // DİKKAT: orijinal email aynı isimle başka tenant tarafından alınmış olabilir,
        // bu durumda restore başarısız olabilir. Owner manuel düzeltsin.
        if (tenant.getEmail() != null && tenant.getEmail().contains(".deleted.")) {
            tenant.setEmail(tenant.getEmail().replaceAll("\\.deleted\\.\\d+$", ""));
        }
        if (tenant.getSlug() != null && tenant.getSlug().contains(".deleted.")) {
            tenant.setSlug(tenant.getSlug().replaceAll("\\.deleted\\.\\d+$", ""));
        }
        tenantRepository.save(tenant);

        List<Kullanici> users = kullaniciRepository.findByTenantId(tenantId);
        for (Kullanici k : users) {
            k.setDeletedAt(null);
            k.setAktif(true);
            if (k.getEmail() != null && k.getEmail().contains(".deleted.")) {
                k.setEmail(k.getEmail().replaceAll("\\.deleted\\.\\d+$", ""));
            }
        }
        kullaniciRepository.saveAll(users);

        auditService.log("ACCOUNT_RESTORE", "Tenant", tenantId,
                Map.of("tenantAd", tenant.getAd()));
    }
}
