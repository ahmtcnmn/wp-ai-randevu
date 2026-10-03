package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.GeriBildirimRequest;
import com.appointflow.dto.GeriBildirimResponse;
import com.appointflow.entity.GeriBildirim;
import com.appointflow.entity.Randevu;
import com.appointflow.entity.RandevuDurumu;
import com.appointflow.repository.GeriBildirimRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeriBildirimService {

    private final GeriBildirimRepository geriBildirimRepository;
    private final RandevuRepository randevuRepository;

    public GeriBildirimResponse geriBildirimEkle(GeriBildirimRequest request, String userEmail) {
        Randevu randevu = randevuRepository.findById(request.getRandevuId())
                .orElseThrow(() -> ApiException.notFound("Randevu bulunamadi."));

        // Tenant izolasyonu
        if (!randevu.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu randevuya erisim yetkiniz yok.");
        }

        if (randevu.getDurum() != RandevuDurumu.TAMAMLANDI) {
            throw ApiException.badRequest("Sadece tamamlanmis randevulara yorum yapabilirsiniz.");
        }

        if (geriBildirimRepository.findByRandevuId(randevu.getId()).isPresent()) {
            throw ApiException.conflict("Bu randevu icin zaten geri bildirim verilmis.");
        }

        boolean sikayetMi = (request.getPuan() <= 2)
                || (request.getSikayetOlarakIsaretle() != null && request.getSikayetOlarakIsaretle());

        GeriBildirim geriBildirim = GeriBildirim.builder()
                .randevu(randevu)
                .puan(request.getPuan())
                .yorum(request.getYorum())
                .sikayetVarmi(sikayetMi)
                .tarih(LocalDateTime.now())
                .build();

        return toResponse(geriBildirimRepository.save(geriBildirim));
    }

    public List<GeriBildirimResponse> uzmaninYorumlariniGetir(Long uzmanId) {
        return geriBildirimRepository.findByRandevuUzmanId(uzmanId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<GeriBildirimResponse> sadeceSikayetleriGetir() {
        Long tenantId = TenantContext.getTenantId();
        return geriBildirimRepository.findBySikayetVarmiTrue()
                .stream()
                .filter(g -> g.getRandevu().getTenantId().equals(tenantId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private GeriBildirimResponse toResponse(GeriBildirim g) {
        return GeriBildirimResponse.builder()
                .id(g.getId())
                .randevuId(g.getRandevu().getId())
                .uzmanId(g.getRandevu().getUzman().getId())
                .uzmanAd(g.getRandevu().getUzman().getAd() + " " + g.getRandevu().getUzman().getSoyad())
                .puan(g.getPuan())
                .yorum(g.getYorum())
                .sikayetVarmi(g.getSikayetVarmi())
                .tarih(g.getTarih())
                .build();
    }
}
