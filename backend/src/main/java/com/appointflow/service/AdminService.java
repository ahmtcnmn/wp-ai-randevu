package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.RaporDTO;
import com.appointflow.entity.Randevu;
import com.appointflow.entity.RandevuDurumu;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RandevuRepository randevuRepository;
    private final KullaniciRepository kullaniciRepository;

    public RaporDTO genelRaporGetir(LocalDateTime baslangic, LocalDateTime bitis) {
        Long tenantId = TenantContext.getTenantId();
        List<Randevu> randevular = randevuRepository.findByTenantId(tenantId).stream()
                .filter(r -> !r.getTarihSaat().isBefore(baslangic) && !r.getTarihSaat().isAfter(bitis))
                .toList();
        return hesaplaRapor(randevular);
    }

    public RaporDTO uzmanRaporGetir(Long uzmanId, LocalDateTime baslangic, LocalDateTime bitis) {
        Long tenantId = TenantContext.getTenantId();
        kullaniciRepository.findById(uzmanId)
                .filter(k -> k.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.notFound("Uzman bulunamadi."));

        List<Randevu> randevular = randevuRepository.findByUzmanId(uzmanId).stream()
                .filter(r -> r.getTenantId().equals(tenantId))
                .filter(r -> !r.getTarihSaat().isBefore(baslangic) && !r.getTarihSaat().isAfter(bitis))
                .toList();
        return hesaplaRapor(randevular);
    }

    private RaporDTO hesaplaRapor(List<Randevu> randevular) {
        RaporDTO rapor = new RaporDTO();
        double toplamGelir = 0;
        long tamamlanan = 0;
        long iptal = 0;
        long gelmedi = 0;

        for (Randevu r : randevular) {
            if (r.getDurum() == RandevuDurumu.TAMAMLANDI) {
                tamamlanan++;
                if (r.getOdenenTutar() != null) {
                    toplamGelir += r.getOdenenTutar();
                } else if (r.getToplamFiyat() != null) {
                    toplamGelir += r.getToplamFiyat();
                }
            } else if (r.getDurum() == RandevuDurumu.IPTAL_EDILDI) {
                iptal++;
            } else if (r.getDurum() == RandevuDurumu.GELMEDI) {
                gelmedi++;
            }
        }

        rapor.setToplamGelir(toplamGelir);
        rapor.setTamamlananRandevuSayisi(tamamlanan);
        rapor.setIptalRandevuSayisi(iptal);
        rapor.setGelmeyenSoruSayisi(gelmedi);
        return rapor;
    }
}
