package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.FeatureResponse;
import com.appointflow.entity.FeatureDefinition;
import com.appointflow.entity.TenantFeature;
import com.appointflow.repository.FeatureDefinitionRepository;
import com.appointflow.repository.TenantFeatureRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureDefinitionRepository featureDefinitionRepository;
    private final TenantFeatureRepository tenantFeatureRepository;

    public boolean isEnabled(Long tenantId, String featureKey) {
        return tenantFeatureRepository.findByTenantIdAndFeatureKey(tenantId, featureKey)
                .map(TenantFeature::getAktif)
                .orElseGet(() -> featureDefinitionRepository.findByFeatureKey(featureKey)
                        .map(FeatureDefinition::getVarsayilan)
                        .orElse(false));
    }

    public List<FeatureResponse> getAllFeatures() {
        Long tenantId = TenantContext.getTenantId();
        List<FeatureDefinition> definitions = featureDefinitionRepository.findAll();
        Map<String, Boolean> tenantFeatures = tenantFeatureRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(TenantFeature::getFeatureKey, TenantFeature::getAktif));

        return definitions.stream()
                .map(def -> FeatureResponse.builder()
                        .featureKey(def.getFeatureKey())
                        .ad(def.getAd())
                        .aciklama(def.getAciklama())
                        .enabled(tenantFeatures.getOrDefault(def.getFeatureKey(), def.getVarsayilan()))
                        .build())
                .collect(Collectors.toList());
    }

    public FeatureResponse toggleFeature(String featureKey, boolean enabled) {
        Long tenantId = TenantContext.getTenantId();

        FeatureDefinition def = featureDefinitionRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> ApiException.notFound("Feature bulunamadi: " + featureKey));

        TenantFeature tf = tenantFeatureRepository.findByTenantIdAndFeatureKey(tenantId, featureKey)
                .orElseGet(() -> TenantFeature.builder()
                        .tenantId(tenantId)
                        .featureKey(featureKey)
                        .build());

        tf.setAktif(enabled);
        tenantFeatureRepository.save(tf);

        return FeatureResponse.builder()
                .featureKey(def.getFeatureKey())
                .ad(def.getAd())
                .aciklama(def.getAciklama())
                .enabled(enabled)
                .build();
    }
}
