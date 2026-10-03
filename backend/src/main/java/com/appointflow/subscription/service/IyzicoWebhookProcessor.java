package com.appointflow.subscription.service;

import com.appointflow.common.ApiException;
import com.appointflow.config.IyzicoConfig;
import com.appointflow.subscription.dto.IyzicoWebhookEvent;
import com.appointflow.subscription.entity.Invoice;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.InvoiceRepository;
import com.appointflow.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoWebhookProcessor {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionPlanService planService;
    private final IyzicoConfig iyzicoConfig;

    public boolean verifySignature(String payload, String signature) {
        try {
            String secret = iyzicoConfig.getWebhookSecret();
            if (secret == null || secret.isBlank()) {
                if (iyzicoConfig.isDevMode()) {
                    log.warn("Iyzico webhook DEV MODE — imza dogrulamasi atlandi. Production'da app.iyzico.webhook-secret konfigure edilmeli.");
                    return true;
                }
                log.error("Iyzico webhook secret konfigure edilmemis ve dev-mode kapali.");
                return false;
            }
            if (signature == null || signature.isBlank()) {
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(computed);
            // Sabit zamanli karsilastirma — timing attack savunmasi
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Webhook imza doğrulama hatası: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void process(IyzicoWebhookEvent event) {
        log.info("İyzico webhook alındı: eventType={}", event.getEventType());

        switch (event.getEventType()) {
            case "SUBSCRIPTION_ORDER_SUCCESS" -> handleSuccess(event);
            case "SUBSCRIPTION_ORDER_FAILED"  -> handleFailed(event);
            case "SUBSCRIPTION_CANCELLED"     -> handleCancelled(event);
            default -> log.warn("Bilinmeyen webhook eventType: {}", event.getEventType());
        }
    }

    private void handleSuccess(IyzicoWebhookEvent event) {
        Subscription sub = findSubscription(event.getSubscriptionReferenceCode());

        Invoice invoice = Invoice.builder()
                .tenantId(sub.getTenantId())
                .subscription(sub)
                .plan(sub.getPlan())
                .iyzicoPaymentId(event.getPaymentId())
                .tutar(sub.getPlan().getAylikFiyat())
                .paraBirimi("TRY")
                .durum("PAID")
                .donemBaslangic(LocalDate.now())
                .donemBitis(LocalDate.now().plusMonths(1))
                .odemeTarihi(LocalDateTime.now())
                .build();
        invoiceRepository.save(invoice);

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setSonrakiOdemeTarihi(LocalDateTime.now().plusMonths(1));
        sub.setGracePeriodBitis(null);
        subscriptionRepository.save(sub);
    }

    private void handleFailed(IyzicoWebhookEvent event) {
        Subscription sub = findSubscription(event.getSubscriptionReferenceCode());

        Invoice invoice = Invoice.builder()
                .tenantId(sub.getTenantId())
                .subscription(sub)
                .plan(sub.getPlan())
                .iyzicoPaymentId(event.getPaymentId())
                .tutar(sub.getPlan().getAylikFiyat())
                .paraBirimi("TRY")
                .durum("FAILED")
                .donemBaslangic(LocalDate.now())
                .donemBitis(LocalDate.now().plusMonths(1))
                .hataMesaji(event.getErrorMessage())
                .build();
        invoiceRepository.save(invoice);

        sub.setStatus(SubscriptionStatus.PAST_DUE);
        sub.setGracePeriodBitis(LocalDateTime.now().plusDays(7));
        subscriptionRepository.save(sub);

        log.warn("Ödeme başarısız: tenantId={}, grace period bitis={}", sub.getTenantId(), sub.getGracePeriodBitis());
    }

    private void handleCancelled(IyzicoWebhookEvent event) {
        Subscription sub = findSubscription(event.getSubscriptionReferenceCode());
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setIptalTarihi(LocalDateTime.now());
        subscriptionRepository.save(sub);

        planService.disableAllPlanFeatures(sub.getTenantId(), sub.getPlan().getPlanKey());
    }

    private Subscription findSubscription(String referenceCode) {
        if (referenceCode == null) {
            throw ApiException.badRequest("Webhook: subscriptionReferenceCode eksik.");
        }
        return subscriptionRepository.findByIyzicoSubscriptionReferenceCode(referenceCode)
                .orElseThrow(() -> ApiException.notFound("Abonelik bulunamadı: " + referenceCode));
    }
}
