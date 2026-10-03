package com.appointflow.subscription.service;

import com.appointflow.notification.dispatcher.NotificationDispatcher;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.repository.SubeRepository;
import com.appointflow.subscription.dto.QuotaUsageResponse;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionPlan;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 1 - B1: Quota field rename + -1 sınırsız semantiği testleri.
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock RandevuRepository randevuRepository;
    @Mock SubeRepository subeRepository;
    @Mock KullaniciRepository kullaniciRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock NotificationDispatcher notificationDispatcher;

    QuotaService service;

    @BeforeEach
    void setUp() {
        service = new QuotaService(subscriptionRepository, randevuRepository, subeRepository,
                kullaniciRepository, redisTemplate, notificationDispatcher);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getUsage_returnsNewFieldNames_andUnlimitedAsMinusOne() {
        Long tenantId = 1L;
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planKey("ENTERPRISE")
                .maxAylikRandevu(null) // null = unlimited
                .maxSube(5)
                .maxCalisan(null)
                .build();
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(plan)
                .status(SubscriptionStatus.ACTIVE).build();

        when(subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(Optional.of(sub));
        when(valueOps.get(anyString())).thenReturn("42"); // 42 used appointments
        when(subeRepository.countByTenantId(tenantId)).thenReturn(2L);
        when(kullaniciRepository.countStaffByTenantId(tenantId)).thenReturn(3L);

        QuotaUsageResponse usage = service.getUsage(tenantId);

        // Yeni field adları (Sprint 1 B1 rename)
        assertThat(usage.getUsedAppointmentsThisMonth()).isEqualTo(42L);
        assertThat(usage.getUsedBranches()).isEqualTo(2L);
        assertThat(usage.getUsedStaff()).isEqualTo(3L);

        // null limit → -1 (sınırsız)
        assertThat(usage.getMaxAppointmentsThisMonth()).isEqualTo(-1L);
        assertThat(usage.getMaxStaff()).isEqualTo(-1L);

        // dolu limit → integer değer
        assertThat(usage.getMaxBranches()).isEqualTo(5L);
    }

    @Test
    void getUsage_finiteLimits_returnsActualNumbers() {
        Long tenantId = 1L;
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planKey("STARTER")
                .maxAylikRandevu(100)
                .maxSube(1)
                .maxCalisan(3)
                .build();
        Subscription sub = Subscription.builder().tenantId(tenantId).plan(plan)
                .status(SubscriptionStatus.TRIALING).build();

        when(subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(Optional.of(sub));
        when(valueOps.get(anyString())).thenReturn("0");
        when(subeRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(kullaniciRepository.countStaffByTenantId(tenantId)).thenReturn(0L);

        QuotaUsageResponse usage = service.getUsage(tenantId);

        assertThat(usage.getMaxAppointmentsThisMonth()).isEqualTo(100L);
        assertThat(usage.getMaxBranches()).isEqualTo(1L);
        assertThat(usage.getMaxStaff()).isEqualTo(3L);
    }
}
