package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.BranchRequest;
import com.appointflow.dto.BranchResponse;
import com.appointflow.entity.Sube;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.repository.SubeRepository;
import com.appointflow.subscription.service.QuotaService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final SubeRepository subeRepository;
    private final KullaniciRepository kullaniciRepository;
    private final RandevuRepository randevuRepository;
    private final QuotaService quotaService;

    public List<BranchResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return subeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BranchResponse getById(Long id) {
        Sube sube = findByIdAndTenant(id);
        return toResponse(sube);
    }

    public BranchResponse create(BranchRequest request) {
        Long tenantId = TenantContext.getTenantId();
        quotaService.assertBranchQuota(tenantId);

        Sube sube = Sube.builder()
                .tenantId(tenantId)
                .ad(request.getAd())
                .adres(request.getAdres())
                .telefon(request.getTelefon())
                .email(request.getEmail())
                .whatsappNumarasi(request.getWhatsappNumarasi())
                .aciklama(request.getAciklama())
                .build();
        subeRepository.save(sube);
        return toResponse(sube);
    }

    public BranchResponse update(Long id, BranchRequest request) {
        Sube sube = findByIdAndTenant(id);
        sube.setAd(request.getAd());
        sube.setAdres(request.getAdres());
        sube.setTelefon(request.getTelefon());
        sube.setEmail(request.getEmail());
        sube.setWhatsappNumarasi(request.getWhatsappNumarasi());
        sube.setAciklama(request.getAciklama());
        subeRepository.save(sube);
        return toResponse(sube);
    }

    /**
     * Şubeyi pasifleştirir. Şu an silme YOK — veri kaybı riski olmasın diye.
     * Çalışan/randevu blokesi de yok; owner pasifleştirip aktif çalışanları başka şubeye taşıyabilir.
     * (Tarihsel veri sorgularken pasif şube hala bağlı görünür.)
     */
    public void deactivate(Long id) {
        Sube sube = findByIdAndTenant(id);
        sube.setAktif(false);
        subeRepository.save(sube);
    }

    public void activate(Long id) {
        Sube sube = findByIdAndTenant(id);
        sube.setAktif(true);
        subeRepository.save(sube);
    }

    private Sube findByIdAndTenant(Long id) {
        Sube sube = subeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sube bulunamadi."));
        if (!sube.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu subeye erisim yetkiniz yok.");
        }
        return sube;
    }

    private BranchResponse toResponse(Sube s) {
        return BranchResponse.builder()
                .id(s.getId())
                .ad(s.getAd())
                .adres(s.getAdres())
                .telefon(s.getTelefon())
                .email(s.getEmail())
                .whatsappNumarasi(s.getWhatsappNumarasi())
                .aciklama(s.getAciklama())
                .aktif(s.getAktif())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
