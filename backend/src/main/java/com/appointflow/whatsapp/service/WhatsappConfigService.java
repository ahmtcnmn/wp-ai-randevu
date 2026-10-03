package com.appointflow.whatsapp.service;

import com.appointflow.common.ApiException;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.dto.WhatsappConfigResponse;
import com.appointflow.whatsapp.dto.WhatsappConfigUpdateRequest;
import com.appointflow.whatsapp.entity.WhatsappConfig;
import com.appointflow.whatsapp.repository.WhatsappConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WhatsappConfigService {

    private final WhatsappConfigRepository repository;

    @Transactional(readOnly = true)
    public WhatsappConfigResponse getConfig() {
        Long tenantId = TenantContext.getTenantId();
        WhatsappConfig config = repository.findByTenantId(tenantId)
                .orElseGet(() -> WhatsappConfig.builder().tenantId(tenantId).build());
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsappConfig> getConfigEntity() {
        Long tenantId = TenantContext.getTenantId();
        return repository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsappConfig> getConfigByPhoneNumberId(String phoneNumberId) {
        return repository.findByPhoneNumberId(phoneNumberId);
    }

    @Transactional
    public WhatsappConfigResponse updateConfig(WhatsappConfigUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        WhatsappConfig config = repository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    WhatsappConfig c = WhatsappConfig.builder().tenantId(tenantId).build();
                    return repository.save(c);
                });

        if (request.getPhoneNumberId() != null) config.setPhoneNumberId(request.getPhoneNumberId());
        if (request.getWabaId() != null) config.setWabaId(request.getWabaId());
        if (request.getAccessToken() != null) config.setAccessToken(request.getAccessToken());
        if (request.getVerifyToken() != null) config.setVerifyToken(request.getVerifyToken());
        if (request.getAppSecret() != null) config.setAppSecret(request.getAppSecret());
        if (request.getWebhookUrl() != null) config.setWebhookUrl(request.getWebhookUrl());
        if (request.getDisplayPhone() != null) config.setDisplayPhone(request.getDisplayPhone());
        if (request.getAktif() != null) config.setAktif(request.getAktif());

        WhatsappConfig saved = repository.save(config);
        return toResponse(saved);
    }

    private WhatsappConfigResponse toResponse(WhatsappConfig config) {
        return WhatsappConfigResponse.builder()
                .id(config.getId())
                .phoneNumberId(config.getPhoneNumberId())
                .wabaId(config.getWabaId())
                .displayPhone(config.getDisplayPhone())
                .webhookUrl(config.getWebhookUrl())
                .aktif(config.getAktif())
                .tokenConfigured(config.getAccessToken() != null && !config.getAccessToken().isBlank())
                .appSecretConfigured(config.getAppSecret() != null && !config.getAppSecret().isBlank())
                .build();
    }
}
