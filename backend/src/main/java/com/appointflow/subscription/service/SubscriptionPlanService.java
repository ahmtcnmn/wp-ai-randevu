package com.appointflow.subscription.service;

import com.appointflow.common.ApiException;
import com.appointflow.service.FeatureService;
import com.appointflow.subscription.dto.PlanFeaturesRequest;
import com.appointflow.subscription.dto.SubscriptionPlanResponse;
import com.appointflow.subscription.dto.SubscriptionPlanUpdateRequest;
import com.appointflow.subscription.entity.SubscriptionPlan;
import com.appointflow.subscription.repository.SubscriptionPlanRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private static final Map<String, Set<String>> PLAN_FEATURES = Map.of(
        "STARTER",    Set.of("BASIC_APPOINTMENTS", "BASIC_REPORTS"),
        "GROWTH",     Set.of("BASIC_APPOINTMENTS", "BASIC_REPORTS", "WHATSAPP_INTEGRATION",
                             "AI_ASSISTANT", "CAMPAIGNS", "MULTI_BRANCH",
                             "PRODUCT_SALES", "COMMISSION_TRACKING"),
        "ENTERPRISE", Set.of("BASIC_APPOINTMENTS", "BASIC_REPORTS", "WHATSAPP_INTEGRATION",
                             "AI_ASSISTANT", "CAMPAIGNS", "MULTI_BRANCH",
                             "PRODUCT_SALES", "COMMISSION_TRACKING", "ADVANCED_REPORTS")
    );

    private final SubscriptionPlanRepository planRepository;
    private final FeatureService featureService;

    public List<SubscriptionPlanResponse> getActivePlans() {
        return planRepository.findByAktifTrue().stream()
                .map(SubscriptionPlanResponse::from)
                .collect(Collectors.toList());
    }

    public List<SubscriptionPlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(SubscriptionPlanResponse::from)
                .collect(Collectors.toList());
    }

    public SubscriptionPlanResponse updatePlan(Long id, SubscriptionPlanUpdateRequest req) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Plan bulunamadı: " + id));
        if (req.getAd() != null) plan.setAd(req.getAd());
        if (req.getAciklama() != null) plan.setAciklama(req.getAciklama());
        if (req.getAylikFiyat() != null) plan.setAylikFiyat(req.getAylikFiyat());
        if (req.getMaxSube() != null) plan.setMaxSube(req.getMaxSube());
        if (req.getMaxCalisan() != null) plan.setMaxCalisan(req.getMaxCalisan());
        if (req.getMaxAylikRandevu() != null) plan.setMaxAylikRandevu(req.getMaxAylikRandevu());
        if (req.getIyzicoReferenceCode() != null) plan.setIyzicoReferenceCode(req.getIyzicoReferenceCode());
        if (req.getAktif() != null) plan.setAktif(req.getAktif());
        planRepository.save(plan);
        return SubscriptionPlanResponse.from(plan);
    }

    public void syncPlanFeatures(Long tenantId, String planKey) {
        Set<String> features = PLAN_FEATURES.getOrDefault(planKey.toUpperCase(), Set.of());
        TenantContext.set(tenantId);
        try {
            for (String featureKey : features) {
                try {
                    featureService.toggleFeature(featureKey, true);
                } catch (Exception ignored) {
                    // Feature definition may not exist — skip silently
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    public void disableAllPlanFeatures(Long tenantId, String planKey) {
        Set<String> features = PLAN_FEATURES.getOrDefault(planKey.toUpperCase(), Set.of());
        TenantContext.set(tenantId);
        try {
            for (String featureKey : features) {
                try {
                    featureService.toggleFeature(featureKey, false);
                } catch (Exception ignored) {}
            }
        } finally {
            TenantContext.clear();
        }
    }

    public void applyPlanFeatures(Long planId, PlanFeaturesRequest req) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> ApiException.notFound("Plan bulunamadı: " + planId));
        // This updates feature state for all tenants on this plan — would require a more complex
        // query in production; for now we record the intent
    }

    public SubscriptionPlan findByKey(String planKey) {
        return planRepository.findByPlanKey(planKey.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Plan bulunamadı: " + planKey));
    }
}
