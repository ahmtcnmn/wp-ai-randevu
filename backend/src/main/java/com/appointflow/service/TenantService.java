package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.CancellationPolicyRequest;
import com.appointflow.dto.CancellationPolicyResponse;
import com.appointflow.dto.TenantResponse;
import com.appointflow.dto.TenantUpdateRequest;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantResponse getCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.badRequest("Tenant bilgisi bulunamadi.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
        return toResponse(tenant);
    }

    public TenantResponse updateTenant(TenantUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.badRequest("Tenant bilgisi bulunamadi.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));

        tenant.setAd(request.getAd());
        tenant.setEmail(request.getEmail());
        tenant.setTelefon(request.getTelefon());
        tenant.setAdres(request.getAdres());
        tenant.setSehir(request.getSehir());
        tenant.setUlke(request.getUlke());
        tenant.setTckn(request.getTckn());
        tenant.setLogoUrl(request.getLogoUrl());
        tenantRepository.save(tenant);

        return toResponse(tenant);
    }

    public CancellationPolicyResponse getCancellationPolicy() {
        Tenant tenant = currentTenant();
        return CancellationPolicyResponse.builder()
                .saatOnce(tenant.getCancellationPolicySaatOnce())
                .mesaj(tenant.getCancellationPolicyMesaj())
                .build();
    }

    public CancellationPolicyResponse updateCancellationPolicy(CancellationPolicyRequest request) {
        Tenant tenant = currentTenant();
        tenant.setCancellationPolicySaatOnce(request.getSaatOnce());
        tenant.setCancellationPolicyMesaj(request.getMesaj());
        tenantRepository.save(tenant);
        return getCancellationPolicy();
    }

    private Tenant currentTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.badRequest("Tenant bilgisi bulunamadi.");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
    }

    public TenantResponse completeOnboarding() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.badRequest("Tenant bilgisi bulunamadi.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant bulunamadi."));
        tenant.setOnboardingCompleted(true);
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    private TenantResponse toResponse(Tenant t) {
        return TenantResponse.builder()
                .id(t.getId())
                .ad(t.getAd())
                .slug(t.getSlug())
                .email(t.getEmail())
                .telefon(t.getTelefon())
                .adres(t.getAdres())
                .sehir(t.getSehir())
                .ulke(t.getUlke())
                .tckn(t.getTckn())
                .logoUrl(t.getLogoUrl())
                .aktif(t.getAktif())
                .onboardingCompleted(t.getOnboardingCompleted())
                .build();
    }
}
