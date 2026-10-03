package com.appointflow.repository;

import com.appointflow.entity.TenantFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureRepository extends JpaRepository<TenantFeature, Long> {
    List<TenantFeature> findByTenantId(Long tenantId);
    Optional<TenantFeature> findByTenantIdAndFeatureKey(Long tenantId, String featureKey);
}
