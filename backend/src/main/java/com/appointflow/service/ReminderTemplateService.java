package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ReminderTemplateRequest;
import com.appointflow.dto.ReminderTemplateResponse;
import com.appointflow.entity.ReminderTemplate;
import com.appointflow.repository.ReminderTemplateRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderTemplateService {

    private final ReminderTemplateRepository repository;

    public List<ReminderTemplateResponse> getAll() {
        return repository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private static final java.util.Set<String> ALLOWED_KANALLAR =
            java.util.Set.of("WHATSAPP", "SMS", "EMAIL");

    public ReminderTemplateResponse create(ReminderTemplateRequest request) {
        ReminderTemplate entity = ReminderTemplate.builder()
                .tenantId(TenantContext.getTenantId())
                .ad(request.getAd())
                .mesaj(request.getMesaj())
                .gunSonra(Math.abs(request.getGunSonra() != null ? request.getGunSonra() : 1))
                .oncesi(Boolean.TRUE.equals(request.getOncesi()))
                .birim(resolveBirim(request.getBirim()))
                .hizmetId(request.getHizmetId())
                .kanal(resolveKanal(request.getKanal()))
                .aktif(request.getAktif() != null ? request.getAktif() : true)
                .build();
        repository.save(entity);
        return toResponse(entity);
    }

    public ReminderTemplateResponse update(Long id, ReminderTemplateRequest request) {
        ReminderTemplate entity = findByIdAndTenant(id);
        entity.setAd(request.getAd());
        entity.setMesaj(request.getMesaj());
        if (request.getGunSonra() != null) entity.setGunSonra(Math.abs(request.getGunSonra()));
        if (request.getOncesi() != null) entity.setOncesi(request.getOncesi());
        if (request.getBirim() != null) entity.setBirim(resolveBirim(request.getBirim()));
        entity.setHizmetId(request.getHizmetId());
        if (request.getKanal() != null) entity.setKanal(resolveKanal(request.getKanal()));
        if (request.getAktif() != null) entity.setAktif(request.getAktif());
        repository.save(entity);
        return toResponse(entity);
    }

    private static final java.util.Set<String> ALLOWED_BIRIMLER = java.util.Set.of("GUN", "SAAT");

    private String resolveBirim(String input) {
        if (input == null) return "GUN";
        String upper = input.toUpperCase();
        if (!ALLOWED_BIRIMLER.contains(upper)) {
            throw ApiException.badRequest("Gecersiz birim: " + upper + ". Izinli: GUN, SAAT");
        }
        return upper;
    }

    private String resolveKanal(String input) {
        if (input == null) return "WHATSAPP";
        String upper = input.toUpperCase();
        if (!ALLOWED_KANALLAR.contains(upper)) {
            throw ApiException.badRequest("Gecersiz kanal: " + upper + ". Izinli: " + ALLOWED_KANALLAR);
        }
        return upper;
    }

    public void delete(Long id) {
        ReminderTemplate entity = findByIdAndTenant(id);
        entity.setAktif(false);
        repository.save(entity);
    }

    private ReminderTemplate findByIdAndTenant(Long id) {
        ReminderTemplate entity = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sablon bulunamadi."));
        if (!entity.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu sablona erisim yetkiniz yok.");
        }
        return entity;
    }

    private ReminderTemplateResponse toResponse(ReminderTemplate e) {
        return ReminderTemplateResponse.builder()
                .id(e.getId())
                .ad(e.getAd())
                .mesaj(e.getMesaj())
                .gunSonra(e.getGunSonra())
                .oncesi(Boolean.TRUE.equals(e.getOncesi()))
                .birim(e.getBirim() != null ? e.getBirim() : "GUN")
                .hizmetId(e.getHizmetId())
                .kanal(e.getKanal())
                .aktif(e.getAktif())
                .build();
    }
}
