package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ServiceRequest;
import com.appointflow.dto.ServiceResponse;
import com.appointflow.entity.Hizmet;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.ServiceCategory;
import com.appointflow.entity.StaffService;
import com.appointflow.repository.HizmetRepository;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.ServiceCategoryRepository;
import com.appointflow.repository.StaffServiceRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HizmetService {

    private final HizmetRepository hizmetRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final StaffServiceRepository staffServiceRepository;
    private final KullaniciRepository kullaniciRepository;

    public List<ServiceResponse> getAll() {
        return hizmetRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponse getById(Long id) {
        return toResponse(findByIdAndTenant(id));
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        Hizmet hizmet = Hizmet.builder()
                .tenantId(TenantContext.getTenantId())
                .ad(request.getAd())
                .aciklama(request.getAciklama())
                .sureDakika(request.getSureDakika())
                .fiyat(request.getFiyat())
                .bufferOnceDk(request.getBufferOnceDk() != null ? request.getBufferOnceDk() : 0)
                .bufferSonraDk(request.getBufferSonraDk() != null ? request.getBufferSonraDk() : 0)
                .takvimRengi(request.getTakvimRengi() != null ? request.getTakvimRengi() : "#3B82F6")
                .build();

        if (request.getKategoriId() != null) {
            ServiceCategory cat = categoryRepository.findById(request.getKategoriId())
                    .orElseThrow(() -> ApiException.notFound("Kategori bulunamadi."));
            hizmet.setKategori(cat);
        }

        hizmetRepository.save(hizmet);
        syncStaffServices(hizmet, request.getStaffIds());
        return toResponse(hizmet);
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceRequest request) {
        Hizmet hizmet = findByIdAndTenant(id);
        hizmet.setAd(request.getAd());
        hizmet.setAciklama(request.getAciklama());
        hizmet.setSureDakika(request.getSureDakika());
        hizmet.setFiyat(request.getFiyat());
        if (request.getBufferOnceDk() != null) hizmet.setBufferOnceDk(request.getBufferOnceDk());
        if (request.getBufferSonraDk() != null) hizmet.setBufferSonraDk(request.getBufferSonraDk());
        if (request.getTakvimRengi() != null) hizmet.setTakvimRengi(request.getTakvimRengi());

        if (request.getKategoriId() != null) {
            ServiceCategory cat = categoryRepository.findById(request.getKategoriId())
                    .orElseThrow(() -> ApiException.notFound("Kategori bulunamadi."));
            hizmet.setKategori(cat);
        } else {
            hizmet.setKategori(null);
        }

        hizmetRepository.save(hizmet);
        if (request.getStaffIds() != null) {
            syncStaffServices(hizmet, request.getStaffIds());
        }
        return toResponse(hizmet);
    }

    private void syncStaffServices(Hizmet hizmet, List<Long> staffIds) {
        Long tenantId = TenantContext.getTenantId();
        List<StaffService> mevcut = staffServiceRepository.findByHizmetId(hizmet.getId());
        staffServiceRepository.deleteAll(mevcut);
        if (staffIds == null || staffIds.isEmpty()) {
            return;
        }
        for (Long staffId : staffIds) {
            Kullanici k = kullaniciRepository.findById(staffId)
                    .orElseThrow(() -> ApiException.notFound("Calisan bulunamadi: " + staffId));
            if (!tenantId.equals(k.getTenantId())) {
                throw ApiException.notFound("Calisan bulunamadi: " + staffId);
            }
            staffServiceRepository.save(StaffService.builder()
                    .kullanici(k)
                    .hizmet(hizmet)
                    .build());
        }
    }

    public void delete(Long id) {
        Hizmet hizmet = findByIdAndTenant(id);
        hizmet.setAktif(false);
        hizmetRepository.save(hizmet);
    }

    public void activate(Long id) {
        Hizmet hizmet = findByIdAndTenant(id);
        hizmet.setAktif(true);
        hizmetRepository.save(hizmet);
    }

    private Hizmet findByIdAndTenant(Long id) {
        Hizmet h = hizmetRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Hizmet bulunamadi."));
        if (!h.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu hizmete erisim yetkiniz yok.");
        }
        return h;
    }

    private ServiceResponse toResponse(Hizmet h) {
        List<Long> staffIds = staffServiceRepository.findByHizmetId(h.getId()).stream()
                .map(ss -> ss.getKullanici().getId())
                .collect(Collectors.toList());
        return ServiceResponse.builder()
                .id(h.getId())
                .ad(h.getAd())
                .aciklama(h.getAciklama())
                .sureDakika(h.getSureDakika())
                .fiyat(h.getFiyat())
                .kategoriId(h.getKategori() != null ? h.getKategori().getId() : null)
                .kategoriAd(h.getKategori() != null ? h.getKategori().getAd() : null)
                .bufferOnceDk(h.getBufferOnceDk())
                .bufferSonraDk(h.getBufferSonraDk())
                .takvimRengi(h.getTakvimRengi())
                .aktif(h.getAktif())
                .staffIds(staffIds)
                .build();
    }
}
