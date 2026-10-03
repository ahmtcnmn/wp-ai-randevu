package com.appointflow.subscription.repository;

import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Optional<Subscription> findByIyzicoSubscriptionReferenceCode(String referenceCode);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.gracePeriodBitis < :now")
    List<Subscription> findExpiredGracePeriod(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIALING' AND s.denemeBitisTarihi < :now")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);

    List<Subscription> findByStatus(SubscriptionStatus status);
}
