package com.appointflow.subscription.repository;

import com.appointflow.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByPlanKey(String planKey);
    List<SubscriptionPlan> findByAktifTrue();
}
