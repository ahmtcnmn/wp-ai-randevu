package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ServiceCategoryRequest;
import com.appointflow.dto.ServiceCategoryResponse;
import com.appointflow.entity.ServiceCategory;
import com.appointflow.repository.HizmetRepository;
import com.appointflow.repository.ServiceCategoryRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private final ServiceCategoryRepository repository;
    private final HizmetRepository hizmetRepository;

    public List<ServiceCategoryResponse> getAll() {
        return repository.findByTenantIdOrderBySiraAsc(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ServiceCategoryResponse create(ServiceCategoryRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<ServiceCategory> existing = repository.findByTenantIdOrderBySiraAsc(tenantId);
        int nextSira = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSira() + 1;

        ServiceCategory entity = ServiceCategory.builder()
                .tenantId(tenantId)
                .ad(request.getAd())
                .aciklama(request.getAciklama())
                .sira(nextSira)
                .takvimRengi(request.getTakvimRengi() != null ? request.getTakvimRengi() : "#3B82F6")
                .build();
        repository.save(entity);
        return toResponse(entity);
    }

    public ServiceCategoryResponse update(Long id, ServiceCategoryRequest request) {
        ServiceCategory entity = findByIdAndTenant(id);
        entity.setAd(request.getAd());
        entity.setAciklama(request.getAciklama());
        if (request.getTakvimRengi() != null) entity.setTakvimRengi(request.getTakvimRengi());
        repository.save(entity);
        return toResponse(entity);
    }

    public void reorder(List<Long> ids) {
        Long tenantId = TenantContext.getTenantId();
        for (int i = 0; i < ids.size(); i++) {
            ServiceCategory cat = repository.findById(ids.get(i))
                    .orElseThrow(() -> ApiException.notFound("Kategori bulunamadi."));
            if (!cat.getTenantId().equals(tenantId)) {
                throw ApiException.forbidden("Bu kategoriye erisim yetkiniz yok.");
            }
            cat.setSira(i);
            repository.save(cat);
        }
    }

    public void delete(Long id) {
        ServiceCategory entity = findByIdAndTenant(id);

        long aktifHizmetSayisi = hizmetRepository.countByKategoriIdAndAktifTrue(id);
        if (aktifHizmetSayisi > 0) {
            throw ApiException.badRequest(
                    "Bu kategoride " + aktifHizmetSayisi + " aktif hizmet var. "
                    + "Önce hizmetleri başka bir kategoriye taşıyın veya pasifleştirin.");
        }

        entity.setAktif(false);
        repository.save(entity);
    }

    private ServiceCategory findByIdAndTenant(Long id) {
        ServiceCategory entity = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Kategori bulunamadi."));
        if (!entity.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu kategoriye erisim yetkiniz yok.");
        }
        return entity;
    }

    private ServiceCategoryResponse toResponse(ServiceCategory e) {
        return ServiceCategoryResponse.builder()
                .id(e.getId())
                .ad(e.getAd())
                .aciklama(e.getAciklama())
                .sira(e.getSira())
                .takvimRengi(e.getTakvimRengi())
                .aktif(e.getAktif())
                .build();
    }
}
