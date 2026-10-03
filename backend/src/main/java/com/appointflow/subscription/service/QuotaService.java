package com.appointflow.subscription.service;

import com.appointflow.common.ApiException;
import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.repository.SubeRepository;
import com.appointflow.subscription.dto.QuotaUsageResponse;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionPlan;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QuotaService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SubscriptionRepository subscriptionRepository;
    private final RandevuRepository randevuRepository;
    private final SubeRepository subeRepository;
    private final KullaniciRepository kullaniciRepository;
    private final StringRedisTemplate redisTemplate;
    private final NotificationDispatcher notificationDispatcher;

    @Autowired
    public QuotaService(SubscriptionRepository subscriptionRepository,
                        RandevuRepository randevuRepository,
                        SubeRepository subeRepository,
                        KullaniciRepository kullaniciRepository,
                        StringRedisTemplate redisTemplate,
                        @Lazy NotificationDispatcher notificationDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.randevuRepository = randevuRepository;
        this.subeRepository = subeRepository;
        this.kullaniciRepository = kullaniciRepository;
        this.redisTemplate = redisTemplate;
        this.notificationDispatcher = notificationDispatcher;
    }

    // ---- Assert methods ----

    public void assertAppointmentQuota(Long tenantId) {
        SubscriptionPlan plan = getActivePlan(tenantId);
        if (plan.getMaxAylikRandevu() == null) return; // unlimited

        long used = getMonthlyAppointmentCount(tenantId);
        int limit = plan.getMaxAylikRandevu();

        if (used >= limit) {
            notificationDispatcher.notifyOwner(tenantId,
                    NotificationDispatcher.Tip.QUOTA_EXCEEDED,
                    "Aylik randevu kotaniz doldu",
                    String.format("%d/%d. Planinizi yukseltmek icin Ayarlar > Plan ekranina gidin.", used, limit),
                    "/ayarlar/plan");
            throw ApiException.quotaExceeded(
                "Aylık randevu kotanıza ulaştınız (" + limit + "). Planı yükseltin.");
        }

        // %80 esigi — yalniz tek seferlik (Redis flag 24 saat)
        if (used + 1 >= (limit * 80) / 100) {
            String warnKey = "quota:warn80:tenant:" + tenantId + ":" + java.time.YearMonth.now().format(MONTH_FMT);
            Boolean alreadyWarned = redisTemplate.hasKey(warnKey);
            if (Boolean.FALSE.equals(alreadyWarned)) {
                redisTemplate.opsForValue().set(warnKey, "1", java.time.Duration.ofDays(35));
                notificationDispatcher.notifyOwner(tenantId,
                        NotificationDispatcher.Tip.QUOTA_WARNING,
                        "Aylik randevu kotaniz %80",
                        String.format("Bu ay %d/%d randevu olusturuldu. Yakinda dolacak.", used + 1, limit),
                        "/ayarlar/plan");
            }
        }
    }

    public void assertBranchQuota(Long tenantId) {
        SubscriptionPlan plan = getActivePlan(tenantId);
        if (plan.getMaxSube() == null) return;

        long count = subeRepository.countByTenantId(tenantId);
        if (count >= plan.getMaxSube()) {
            throw ApiException.quotaExceeded(
                "Şube kotanıza ulaştınız (" + plan.getMaxSube() + "). Planı yükseltin.");
        }
    }

    public void assertStaffQuota(Long tenantId) {
        SubscriptionPlan plan = getActivePlan(tenantId);
        if (plan.getMaxCalisan() == null) return;

        long count = kullaniciRepository.countStaffByTenantId(tenantId);
        if (count >= plan.getMaxCalisan()) {
            throw ApiException.quotaExceeded(
                "Çalışan kotanıza ulaştınız (" + plan.getMaxCalisan() + "). Planı yükseltin.");
        }
    }

    // ---- Redis counter management ----

    public void incrementAppointmentCounter(Long tenantId) {
        String key = appointmentKey(tenantId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 35, TimeUnit.DAYS);
    }

    public void decrementAppointmentCounter(Long tenantId) {
        String key = appointmentKey(tenantId);
        Long newVal = redisTemplate.opsForValue().decrement(key);
        // Negatife dusmesin (consistency korumasi)
        if (newVal != null && newVal < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
        redisTemplate.expire(key, 35, TimeUnit.DAYS);
    }

    public void reconcileCounter(Long tenantId, int actualCount) {
        String key = appointmentKey(tenantId);
        redisTemplate.opsForValue().set(key, String.valueOf(actualCount));
        redisTemplate.expire(key, 35, TimeUnit.DAYS);
    }

    public QuotaUsageResponse getUsage(Long tenantId) {
        SubscriptionPlan plan = getActivePlan(tenantId);
        long appointmentsUsed = getMonthlyAppointmentCount(tenantId);
        long branchCount = subeRepository.countByTenantId(tenantId);
        long staffCount = kullaniciRepository.countStaffByTenantId(tenantId);

        return QuotaUsageResponse.builder()
                .usedAppointmentsThisMonth(appointmentsUsed)
                .maxAppointmentsThisMonth(asLimit(plan.getMaxAylikRandevu()))
                .usedBranches(branchCount)
                .maxBranches(asLimit(plan.getMaxSube()))
                .usedStaff(staffCount)
                .maxStaff(asLimit(plan.getMaxCalisan()))
                .build();
    }

    private long asLimit(Integer value) {
        return value == null ? -1L : value.longValue();
    }

    // ---- Helpers ----

    private long getMonthlyAppointmentCount(Long tenantId) {
        String key = appointmentKey(tenantId);
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) {
            return Long.parseLong(val);
        }
        // Fallback to DB
        YearMonth ym = YearMonth.now();
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
        long count = randevuRepository.countByTenantIdAndPeriod(tenantId, from, to);
        reconcileCounter(tenantId, (int) count);
        return count;
    }

    private SubscriptionPlan getActivePlan(Long tenantId) {
        return subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                          || s.getStatus() == SubscriptionStatus.TRIALING
                          || s.getStatus() == SubscriptionStatus.PAST_DUE)
                .map(Subscription::getPlan)
                .orElseThrow(() -> ApiException.quotaExceeded("Aktif abonelik bulunamadı. Lütfen plan seçin."));
    }

    private String appointmentKey(Long tenantId) {
        return "quota:tenant:" + tenantId + ":appointments:" + YearMonth.now().format(MONTH_FMT);
    }
}
