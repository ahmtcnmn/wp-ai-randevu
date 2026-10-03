package com.appointflow.subscription.service;

import com.appointflow.common.ApiException;
import com.appointflow.config.IyzicoConfig;
import com.appointflow.entity.Tenant;
import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.TenantRepository;
import com.appointflow.subscription.dto.*;
import com.appointflow.subscription.entity.Invoice;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionPlan;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.InvoiceRepository;
import com.appointflow.subscription.repository.SubscriptionRepository;
import com.appointflow.tenant.TenantContext;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionPlanService planService;
    private final KullaniciRepository kullaniciRepository;
    private final TenantRepository tenantRepository;
    private final Options iyzicoOptions;
    private final IyzicoConfig iyzicoConfig;
    private final StringRedisTemplate redisTemplate;
    private final NotificationDispatcher notificationDispatcher;
    private final com.appointflow.audit.service.AuditService auditService;

    public CheckoutInitResponse initializeCheckout(Long tenantId, String planKey) {
        SubscriptionPlan plan = planService.findByKey(planKey);

        // Find owner email for buyer info
        var owner = kullaniciRepository.findByTenantId(tenantId).stream()
                .filter(k -> k.getRol().name().equals("OWNER"))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Tenant sahibi bulunamadı."));

        String ownerEmail = owner.getEmail() != null ? owner.getEmail().trim().toLowerCase() : null;
        if (ownerEmail == null || ownerEmail.isEmpty()
                || !ownerEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw ApiException.badRequest(
                    "Hesabinizda gecerli bir e-posta yok. Lutfen Ayarlar > Hesap'tan e-posta bilginizi guncelleyin.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadı."));

        String tckn = nullIfBlank(tenant.getTckn());
        String adres = nullIfBlank(tenant.getAdres());
        String sehir = nullIfBlank(tenant.getSehir());
        String ulke = nullIfBlank(tenant.getUlke());

        if (iyzicoConfig.isStrictBuyerInfo()) {
            List<String> eksikler = new ArrayList<>();
            if (tckn == null || tckn.length() != 11) eksikler.add("TC Kimlik No (11 hane)");
            if (adres == null) eksikler.add("adres");
            if (sehir == null) eksikler.add("sehir");
            if (ulke == null) eksikler.add("ulke");
            if (!eksikler.isEmpty()) {
                throw ApiException.badRequest(
                        "Odeme baslatilamadi. Eksik fatura bilgileri: " + String.join(", ", eksikler)
                                + ". Lutfen Ayarlar > Isletme bolumunden tamamlayin.");
            }
        }

        // Sandbox / dev fallback'leri — production'da strict-buyer-info=true zorunlu
        if (tckn == null || tckn.length() != 11) tckn = "11111111111";
        if (adres == null) adres = "Adres";
        if (sehir == null) sehir = "Istanbul";
        if (ulke == null) ulke = "Turkey";

        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId("tenant-" + tenantId + "-plankey-" + plan.getPlanKey());
        request.setPrice(plan.getAylikFiyat());
        request.setPaidPrice(plan.getAylikFiyat());
        request.setCurrency(Currency.TRY.name());
        request.setCallbackUrl(iyzicoConfig.getCallbackUrl());
        request.setEnabledInstallments(List.of(1));

        Buyer buyer = new Buyer();
        buyer.setId("tenant-" + tenantId);
        buyer.setName(nullOrPlaceholder(owner.getAd(), "Ad"));
        buyer.setSurname(nullOrPlaceholder(owner.getSoyad(), "Soyad"));
        buyer.setEmail(ownerEmail);
        buyer.setIdentityNumber(tckn);
        buyer.setRegistrationAddress(adres);
        buyer.setCity(sehir);
        buyer.setCountry(ulke);
        // İyzico GSM format zorunlu: +90... veya 10-15 hane. Hatalı format gönderirsen 5008 hatası alırsın.
        String gsm = normalizeGsmForIyzico(owner.getTelefon());
        if (gsm != null) buyer.setGsmNumber(gsm);
        request.setBuyer(buyer);

        log.info("İyzico checkout request: tenantId={}, email={}, gsm={}, plan={}",
                tenantId, ownerEmail, gsm, plan.getPlanKey());

        Address shippingAddress = new Address();
        shippingAddress.setContactName(owner.getAd() + " " + owner.getSoyad());
        shippingAddress.setCity(sehir);
        shippingAddress.setCountry(ulke);
        shippingAddress.setAddress(adres);
        request.setShippingAddress(shippingAddress);
        request.setBillingAddress(shippingAddress);

        BasketItem basketItem = new BasketItem();
        basketItem.setId("plan-" + plan.getId());
        basketItem.setName(plan.getAd() + " - Aylık Abonelik");
        basketItem.setCategory1("Abonelik");
        basketItem.setItemType(BasketItemType.VIRTUAL.name());
        basketItem.setPrice(plan.getAylikFiyat());

        List<BasketItem> basketItems = new ArrayList<>();
        basketItems.add(basketItem);
        request.setBasketItems(basketItems);

        CheckoutFormInitialize checkoutForm = CheckoutFormInitialize.create(request, iyzicoOptions);

        if (!"success".equalsIgnoreCase(checkoutForm.getStatus())) {
            log.error("İyzico checkout başlatılamadı: {}", checkoutForm.getErrorMessage());
            throw ApiException.badRequest("Ödeme formu oluşturulamadı: " + checkoutForm.getErrorMessage());
        }

        // Token → tenantId:planKey mapping'i Redis'e kaydet (2 saat TTL)
        String redisKey = "checkout:token:" + checkoutForm.getToken();
        redisTemplate.opsForValue().set(redisKey, tenantId + ":" + planKey, Duration.ofHours(2));

        return CheckoutInitResponse.builder()
                .checkoutFormContent(checkoutForm.getCheckoutFormContent())
                .token(checkoutForm.getToken())
                .planKey(planKey)
                .build();
    }

    @Transactional
    public SubscriptionResponse verifyAndActivate(Long tenantId, String token, String planKey) {
        RetrieveCheckoutFormRequest retrieveRequest = new RetrieveCheckoutFormRequest();
        retrieveRequest.setLocale(Locale.TR.getValue());
        retrieveRequest.setToken(token);

        CheckoutForm form = CheckoutForm.retrieve(retrieveRequest, iyzicoOptions);

        if (!"success".equalsIgnoreCase(form.getStatus()) || !"SUCCESS".equalsIgnoreCase(form.getPaymentStatus())) {
            throw ApiException.badRequest("Ödeme doğrulanamadı: " + form.getErrorMessage());
        }

        SubscriptionPlan plan = planService.findByKey(planKey);

        // Mevcut aktif aboneliği iptal et
        subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(s -> s.getStatus() == SubscriptionStatus.TRIALING || s.getStatus() == SubscriptionStatus.ACTIVE)
                .ifPresent(s -> {
                    s.setStatus(SubscriptionStatus.CANCELLED);
                    s.setIptalTarihi(LocalDateTime.now());
                    subscriptionRepository.save(s);
                });

        Subscription subscription = Subscription.builder()
                .tenantId(tenantId)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .baslangicTarihi(LocalDateTime.now())
                .sonrakiOdemeTarihi(LocalDateTime.now().plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .subscription(subscription)
                .plan(plan)
                .iyzicoPaymentId(form.getPaymentId())
                .iyzicoToken(token)
                .tutar(plan.getAylikFiyat())
                .paraBirimi("TRY")
                .durum("PAID")
                .donemBaslangic(LocalDate.now())
                .donemBitis(LocalDate.now().plusMonths(1))
                .odemeTarihi(LocalDateTime.now())
                .build();
        invoiceRepository.save(invoice);

        planService.syncPlanFeatures(tenantId, planKey);

        notificationDispatcher.notifyOwner(tenantId,
                NotificationDispatcher.Tip.PAYMENT_SUCCESS,
                "Odeme basarili",
                String.format("%s planiniz aktif. Tutar: %s TRY", plan.getAd(), plan.getAylikFiyat()),
                "/ayarlar/plan");

        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long tenantId) {
        Subscription sub = subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> ApiException.notFound("Aktif abonelik bulunamadı."));

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setIptalTarihi(LocalDateTime.now());
        subscriptionRepository.save(sub);

        planService.disableAllPlanFeatures(tenantId, sub.getPlan().getPlanKey());

        auditService.log("SUBSCRIPTION_CANCEL", "Subscription", sub.getId(),
                java.util.Map.of("planKey", sub.getPlan().getPlanKey()));

        return SubscriptionResponse.from(sub);
    }

    public CheckoutInitResponse upgradeSubscription(Long tenantId, String newPlanKey) {
        return initializeCheckout(tenantId, newPlanKey);
    }

    public SubscriptionResponse getSubscription(Long tenantId) {
        Subscription sub = subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> ApiException.notFound("Abonelik bulunamadı."));
        return SubscriptionResponse.from(sub);
    }

    public List<InvoiceResponse> getInvoices(Long tenantId) {
        return invoiceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    /**
     * İyzico POST callback handler.
     * Form token ve conversationId (içinde planKey var) ile ödemeyi doğrular.
     * Başarılıysa tenantId'yi conversationId'den çıkarır, aboneliği aktifler.
     * Sonucu frontend callback URL'ine redirect eder.
     */
    @Transactional
    public String handleIyzicoCallback(String token, String conversationIdParam) {
        String frontendBase = iyzicoConfig.getFrontendCallbackUrl(); // http://localhost:3000/billing/callback

        if (token == null || token.isBlank()) {
            return frontendBase + "?error=missing_token";
        }

        try {
            // Token → tenantId:planKey'i Redis'ten al
            String redisKey = "checkout:token:" + token;
            String redisValue = redisTemplate.opsForValue().get(redisKey);
            log.info("İyzico callback Redis lookup: key={}, value={}", redisKey, redisValue);

            String planKey = null;
            Long tenantId  = null;
            if (redisValue != null && redisValue.contains(":")) {
                String[] parts = redisValue.split(":", 2);
                tenantId = Long.parseLong(parts[0]);
                planKey  = parts[1].toUpperCase();
            } else {
                log.warn("Checkout token Redis'te bulunamadı: {}", token);
                return frontendBase + "?status=failed&error=session_expired";
            }

            RetrieveCheckoutFormRequest retrieveRequest = new RetrieveCheckoutFormRequest();
            retrieveRequest.setLocale(Locale.TR.getValue());
            retrieveRequest.setToken(token);

            CheckoutForm form = CheckoutForm.retrieve(retrieveRequest, iyzicoOptions);
            log.info("İyzico retrieve: status={}, paymentStatus={}", form.getStatus(), form.getPaymentStatus());

            if ("success".equalsIgnoreCase(form.getStatus()) && "SUCCESS".equalsIgnoreCase(form.getPaymentStatus())) {
                SubscriptionPlan plan = planService.findByKey(planKey);

                subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                        .filter(s -> s.getStatus() == SubscriptionStatus.TRIALING || s.getStatus() == SubscriptionStatus.ACTIVE)
                        .ifPresent(s -> { s.setStatus(SubscriptionStatus.CANCELLED); s.setIptalTarihi(LocalDateTime.now()); subscriptionRepository.save(s); });

                Subscription subscription = Subscription.builder()
                        .tenantId(tenantId).plan(plan).status(SubscriptionStatus.ACTIVE)
                        .baslangicTarihi(LocalDateTime.now()).sonrakiOdemeTarihi(LocalDateTime.now().plusMonths(1))
                        .build();
                subscriptionRepository.save(subscription);

                Invoice invoice = Invoice.builder()
                        .tenantId(tenantId).subscription(subscription).plan(plan)
                        .iyzicoPaymentId(form.getPaymentId()).iyzicoToken(token)
                        .tutar(plan.getAylikFiyat()).paraBirimi("TRY").durum("PAID")
                        .donemBaslangic(LocalDate.now()).donemBitis(LocalDate.now().plusMonths(1))
                        .odemeTarihi(LocalDateTime.now()).build();
                invoiceRepository.save(invoice);

                planService.syncPlanFeatures(tenantId, planKey);
                log.info("Ödeme başarılı: tenantId={}, plan={}", tenantId, planKey);

                notificationDispatcher.notifyOwner(tenantId,
                        NotificationDispatcher.Tip.PAYMENT_SUCCESS,
                        "Odeme basarili",
                        String.format("%s planiniz aktif. Tutar: %s TRY", plan.getAd(), plan.getAylikFiyat()),
                        "/ayarlar/plan");

                return frontendBase + "?status=success&plan=" + planKey;
            } else {
                log.warn("Ödeme başarısız: status={}, paymentStatus={}, hata={}", form.getStatus(), form.getPaymentStatus(), form.getErrorMessage());

                if (tenantId != null) {
                    notificationDispatcher.notifyOwner(tenantId,
                            NotificationDispatcher.Tip.PAYMENT_FAILED,
                            "Odeme basarisiz",
                            form.getErrorMessage() != null ? form.getErrorMessage() : "Odeme reddedildi.",
                            "/ayarlar/plan");
                }

                return frontendBase + "?status=failed&error=" + java.net.URLEncoder.encode(
                        form.getErrorMessage() != null ? form.getErrorMessage() : "Ödeme reddedildi", "UTF-8");
            }
        } catch (Exception e) {
            log.error("Callback işleme hatası: {}", e.getMessage());
            return frontendBase + "?status=failed&error=server_error";
        }
    }

    @Transactional
    public void overridePlan(Long tenantId, String planKey) {
        SubscriptionPlan plan = planService.findByKey(planKey);

        subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .ifPresent(s -> {
                    s.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(s);
                });

        Subscription subscription = Subscription.builder()
                .tenantId(tenantId)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .baslangicTarihi(LocalDateTime.now())
                .sonrakiOdemeTarihi(LocalDateTime.now().plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        planService.syncPlanFeatures(tenantId, planKey);
    }

    public List<AdminTenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(tenant -> {
                    var sub = subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenant.getId());
                    return AdminTenantResponse.builder()
                            .id(tenant.getId())
                            .ad(tenant.getAd())
                            .slug(tenant.getSlug())
                            .email(tenant.getEmail())
                            .telefon(tenant.getTelefon())
                            .aktif(tenant.getAktif())
                            .createdAt(tenant.getCreatedAt() != null ? tenant.getCreatedAt().toString() : null)
                            .planKey(sub.map(s -> s.getPlan().getPlanKey()).orElse(null))
                            .subscriptionStatus(sub.map(s -> s.getStatus().name()).orElse(null))
                            .subscriptionEnd(sub.map(s -> s.getSonrakiOdemeTarihi() != null ? s.getSonrakiOdemeTarihi().toLocalDate().toString() : null).orElse(null))
                            .businessType(tenant.getBusinessType() != null ? tenant.getBusinessType().name() : "OTHER")
                            .build();
                })
                .toList();
    }

    public AdminTenantResponse getTenantDetail(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
        var sub = subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId);
        return AdminTenantResponse.builder()
                .id(tenant.getId())
                .ad(tenant.getAd())
                .slug(tenant.getSlug())
                .email(tenant.getEmail())
                .telefon(tenant.getTelefon())
                .aktif(tenant.getAktif())
                .createdAt(tenant.getCreatedAt() != null ? tenant.getCreatedAt().toString() : null)
                .planKey(sub.map(s -> s.getPlan().getPlanKey()).orElse(null))
                .subscriptionStatus(sub.map(s -> s.getStatus().name()).orElse(null))
                .subscriptionEnd(sub.map(s -> s.getSonrakiOdemeTarihi() != null
                        ? s.getSonrakiOdemeTarihi().toLocalDate().toString() : null).orElse(null))
                .businessType(tenant.getBusinessType() != null ? tenant.getBusinessType().name() : "OTHER")
                .build();
    }

    @Transactional
    public void setTenantAktif(Long tenantId, Boolean aktif) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadı."));
        tenant.setAktif(aktif);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void setTenantBusinessType(Long tenantId, com.appointflow.tenant.BusinessType businessType) {
        if (businessType == null) {
            throw ApiException.badRequest("businessType zorunlu.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadı."));
        tenant.setBusinessType(businessType);
        tenantRepository.save(tenant);
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String nullOrPlaceholder(String s, String placeholder) {
        return (s == null || s.isBlank()) ? placeholder : s.trim();
    }

    /**
     * İyzico GSM formatı: +90 ile başlayan +90XXXXXXXXXX (toplam 13 char).
     * Türkiye telefonu farklı formatlarda gelebilir (05xx, 5xx, +905xx, vb.) — normalize et.
     * Geçersizse null döner (İyzico'ya gönderilmez, opsiyonel field).
     */
    private static String normalizeGsmForIyzico(String input) {
        if (input == null || input.isBlank()) return null;
        // Sadece rakam ve + tut
        String digits = input.replaceAll("[^0-9+]", "");
        // +90 prefix yoksa ekle
        if (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.startsWith("90") && digits.length() == 12) return "+" + digits;
        if (digits.startsWith("+90") && digits.length() == 13) return digits;
        if (digits.length() == 10) return "+90" + digits;
        // Geçersiz format — null gönder, İyzico GSM'siz devam etsin
        return null;
    }
}
