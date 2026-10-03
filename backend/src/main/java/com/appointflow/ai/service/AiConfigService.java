package com.appointflow.ai.service;

import com.appointflow.ai.dto.AiConfigResponse;
import com.appointflow.ai.dto.AiConfigUpdateRequest;
import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.repository.AiConfigRepository;
import com.appointflow.common.ApiException;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import com.appointflow.tenant.BusinessType;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public AiConfigResponse getConfig() {
        Long tenantId = TenantContext.getTenantId();
        AiConfig config = aiConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public AiConfig getConfigEntity() {
        Long tenantId = TenantContext.getTenantId();
        return aiConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
    }

    @Transactional
    public AiConfigResponse updateConfig(AiConfigUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        AiConfig config = aiConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    AiConfig c = createDefaultConfig(tenantId);
                    return aiConfigRepository.save(c);
                });

        if (request.getPersonaAdi() != null) config.setPersonaAdi(request.getPersonaAdi());
        if (request.getSistemPromptu() != null) config.setSistemPromptu(request.getSistemPromptu());
        if (request.getIsletmeAciklamasi() != null) config.setIsletmeAciklamasi(request.getIsletmeAciklamasi());
        if (request.getDil() != null) config.setDil(request.getDil());
        if (request.getFiyatBilgisiGoster() != null) config.setFiyatBilgisiGoster(request.getFiyatBilgisiGoster());
        if (request.getOtomatikOnay() != null) config.setOtomatikOnay(request.getOtomatikOnay());
        if (request.getHandoffKelimeleri() != null) config.setHandoffKelimeleri(request.getHandoffKelimeleri());
        if (request.getMaxToken() != null) config.setMaxToken(request.getMaxToken());
        if (request.getModel() != null) config.setModel(request.getModel());
        if (request.getAktif() != null) config.setAktif(request.getAktif());

        AiConfig saved = aiConfigRepository.save(config);
        return toResponse(saved);
    }

    private AiConfig createDefaultConfig(Long tenantId) {
        // Tenant'ın sektörüne göre persona + prompt seç
        BusinessType type = BusinessType.OTHER;
        if (tenantId != null) {
            Tenant t = tenantRepository.findById(tenantId).orElse(null);
            if (t != null && t.getBusinessType() != null) {
                type = t.getBusinessType();
            }
        }
        SectorAiDefaults.SectorAiDefault def = SectorAiDefaults.getDefault(type);
        return AiConfig.builder()
                .tenantId(tenantId)
                .personaAdi(def.personaAdi())
                .sistemPromptu(def.sistemPromptu())
                .dil("tr")
                .fiyatBilgisiGoster(true)
                .otomatikOnay(false)
                .handoffKelimeleri("şikayet,yanlış,berbat,kötü,rezalet,müdür,patron,sahibi")
                .maxToken(1024)
                .model("gpt-4o-mini")
                .aktif(true)
                .build();
    }

    private AiConfigResponse toResponse(AiConfig config) {
        return AiConfigResponse.builder()
                .id(config.getId())
                .personaAdi(config.getPersonaAdi())
                .sistemPromptu(config.getSistemPromptu())
                .isletmeAciklamasi(config.getIsletmeAciklamasi())
                .dil(config.getDil())
                .fiyatBilgisiGoster(config.getFiyatBilgisiGoster())
                .otomatikOnay(config.getOtomatikOnay())
                .handoffKelimeleri(config.getHandoffKelimeleri())
                .maxToken(config.getMaxToken())
                .model(config.getModel())
                .aktif(config.getAktif())
                .build();
    }
}
