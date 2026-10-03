package com.appointflow.repository;

import com.appointflow.entity.FeatureDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureDefinitionRepository extends JpaRepository<FeatureDefinition, Long> {
    Optional<FeatureDefinition> findByFeatureKey(String featureKey);
}
