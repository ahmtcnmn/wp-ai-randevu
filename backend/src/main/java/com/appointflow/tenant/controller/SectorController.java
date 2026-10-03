package com.appointflow.tenant.controller;

import com.appointflow.common.ApiException;
import com.appointflow.common.ApiResponse;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.TenantRepository;
import com.appointflow.tenant.BusinessType;
import com.appointflow.tenant.SectorLabels;
import com.appointflow.tenant.StaffPosition;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sektör etiketleri ve metadata endpoint'i.
 * Web + mobil app açılışta /labels'i çağırıp UI'da kullanır.
 */
@RestController
@RequestMapping("/api/v1/sector")
@RequiredArgsConstructor
public class SectorController {

    private final TenantRepository tenantRepository;

    /**
     * Giriş yapmış kullanıcının tenant'ının sektör etiketleri.
     * Cache: frontend bu endpoint'i session başında 1 kere çağırıp Context'e koyar.
     */
    @GetMapping("/labels")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLabels() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.unauthorized("Oturum bulunamadı.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("İşletme bulunamadı."));

        BusinessType type = tenant.getBusinessType() != null ? tenant.getBusinessType() : BusinessType.OTHER;
        SectorLabels.LabelDictionary labels = SectorLabels.getLabels(type);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("businessType", type.name());
        response.put("businessTypeName", SectorLabels.getDisplayName(type));

        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("staffSingular", labels.staffSingular());
        labelMap.put("staffPlural", labels.staffPlural());
        labelMap.put("customerSingular", labels.customerSingular());
        labelMap.put("customerPlural", labels.customerPlural());
        labelMap.put("serviceSingular", labels.serviceSingular());
        labelMap.put("servicePlural", labels.servicePlural());
        labelMap.put("appointmentSingular", labels.appointmentSingular());
        labelMap.put("appointmentPlural", labels.appointmentPlural());
        response.put("labels", labelMap);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Tenant'ın sektörü için geçerli pozisyon listesi.
     * Frontend "Çalışan ekle" formunda dropdown için kullanır.
     */
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<Object>> getPositions() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw ApiException.unauthorized("Oturum bulunamadı.");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("İşletme bulunamadı."));
        BusinessType type = tenant.getBusinessType() != null ? tenant.getBusinessType() : BusinessType.OTHER;

        java.util.List<Map<String, String>> list = new java.util.ArrayList<>();
        for (StaffPosition p : StaffPosition.forBusinessType(type)) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("code", p.name());
            entry.put("name", p.displayName());
            list.add(entry);
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Tüm desteklenen sektörlerin listesi — register sayfası için (auth gerektirmez).
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Object>> getAllTypes() {
        java.util.List<Map<String, String>> list = new java.util.ArrayList<>();
        for (BusinessType t : BusinessType.values()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("code", t.name());
            entry.put("name", SectorLabels.getDisplayName(t));
            list.add(entry);
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
