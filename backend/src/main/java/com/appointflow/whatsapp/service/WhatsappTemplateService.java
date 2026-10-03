package com.appointflow.whatsapp.service;

import com.appointflow.common.ApiException;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.dto.WhatsappTemplateRequest;
import com.appointflow.whatsapp.dto.WhatsappTemplateResponse;
import com.appointflow.whatsapp.entity.WhatsappTemplate;
import com.appointflow.whatsapp.entity.WhatsappTemplate.TemplateDurum;
import com.appointflow.whatsapp.entity.WhatsappTemplate.TemplateKategori;
import com.appointflow.whatsapp.repository.WhatsappTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappTemplateService {

    private final WhatsappTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<WhatsappTemplateResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return repository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WhatsappTemplateResponse create(WhatsappTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();

        // Ayni key kontrolu
        repository.findByTenantIdAndTemplateKey(tenantId, request.getTemplateKey())
                .ifPresent(t -> {
                    throw ApiException.conflict("Bu template key zaten mevcut: " + request.getTemplateKey());
                });

        WhatsappTemplate template = WhatsappTemplate.builder()
                .tenantId(tenantId)
                .templateName(request.getAd())
                .templateKey(request.getTemplateKey())
                .kategori(parseKategori(request.getKategori()))
                .dil(request.getDil() != null ? request.getDil() : "tr")
                .icerik(request.getGovde())
                .headerText(request.getBaslik())
                .footerText(request.getFooter())
                .durum(TemplateDurum.DRAFT)
                .build();

        WhatsappTemplate saved = repository.save(template);
        log.info("WhatsApp template olusturuldu: {} (key: {})", saved.getTemplateName(), saved.getTemplateKey());
        return toResponse(saved);
    }

    @Transactional
    public WhatsappTemplateResponse update(Long id, WhatsappTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        WhatsappTemplate template = repository.findById(id)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.notFound("Template bulunamadı: " + id));

        if (template.getDurum() == TemplateDurum.APPROVED || template.getDurum() == TemplateDurum.PENDING) {
            throw ApiException.badRequest("Onaylanmış veya bekleyen template güncellenemez. Yeni bir template oluşturun.");
        }

        template.setTemplateName(request.getAd());
        template.setIcerik(request.getGovde());
        if (request.getBaslik() != null) template.setHeaderText(request.getBaslik());
        if (request.getFooter() != null) template.setFooterText(request.getFooter());
        if (request.getKategori() != null) template.setKategori(parseKategori(request.getKategori()));
        if (request.getDil() != null) template.setDil(request.getDil());

        WhatsappTemplate saved = repository.save(template);
        return toResponse(saved);
    }

    @Transactional
    public WhatsappTemplateResponse submitToMeta(Long id) {
        Long tenantId = TenantContext.getTenantId();
        WhatsappTemplate template = repository.findById(id)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.notFound("Template bulunamadı: " + id));

        if (template.getDurum() != TemplateDurum.DRAFT && template.getDurum() != TemplateDurum.REJECTED) {
            throw ApiException.badRequest("Sadece DRAFT veya REJECTED durumundaki template Meta'ya gönderilebilir.");
        }

        // TODO: Meta WhatsApp Business API'ye gercek gonderm
        // Su an sadece durum guncelleme
        template.setDurum(TemplateDurum.PENDING);
        WhatsappTemplate saved = repository.save(template);

        log.info("WhatsApp template Meta'ya gonderildi: {} (key: {})", saved.getTemplateName(), saved.getTemplateKey());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        WhatsappTemplate template = repository.findById(id)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.notFound("Template bulunamadı: " + id));

        repository.delete(template);
        log.info("WhatsApp template silindi: {} (key: {})", template.getTemplateName(), template.getTemplateKey());
    }

    private TemplateKategori parseKategori(String kategori) {
        if (kategori == null) return TemplateKategori.UTILITY;
        try {
            return TemplateKategori.valueOf(kategori.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TemplateKategori.UTILITY;
        }
    }

    private WhatsappTemplateResponse toResponse(WhatsappTemplate template) {
        return WhatsappTemplateResponse.builder()
                .id(template.getId())
                .ad(template.getTemplateName())
                .templateKey(template.getTemplateKey())
                .kategori(template.getKategori().name())
                .dil(template.getDil())
                .govde(template.getIcerik())
                .baslik(template.getHeaderText())
                .footer(template.getFooterText())
                .status(template.getDurum().name())
                .metaTemplateId(template.getMetaTemplateId())
                .redSebebi(template.getRedNedeni())
                .build();
    }
}
