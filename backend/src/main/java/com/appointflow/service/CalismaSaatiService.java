package com.appointflow.service;

import com.appointflow.dto.CalismaSaatiRequest;
import com.appointflow.entity.CalismaSaati;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.repository.CalismaSaatiRepository;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalismaSaatiService {

    private final CalismaSaatiRepository calismaSaatiRepository;
    private final KullaniciRepository kullaniciRepository;

    public List<CalismaSaati> uzmanSaatleriniGetir(Long uzmanId) {
        return calismaSaatiRepository.findByUzmanId(uzmanId);
    }

    public List<CalismaSaati> saatleriKaydet(String uzmanEmail, List<CalismaSaatiRequest> requestList) {
        Kullanici uzman = kullaniciRepository.findByEmail(uzmanEmail)
                .orElseThrow(() -> new RuntimeException("Uzman bulunamadı."));

        // OWNER/ADMIN can save for any staff via uzmanId in request; fallback to self
        Long targetId = requestList.isEmpty() ? uzman.getId() : requestList.get(0).getUzmanId();
        final Kullanici hedefUzman = (targetId != null && !targetId.equals(uzman.getId()))
                ? kullaniciRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("Hedef uzman bulunamadi."))
                : uzman;

        if (hedefUzman.getRol() != Role.STAFF && hedefUzman.getRol() != Role.BRANCH_MANAGER) {
            throw new RuntimeException("Sadece calisanlar calisma saati belirleyebilir.");
        }

        // Önceki saatleri silip yenilerini ekleyebiliriz veya güncelleyebiliriz
        List<CalismaSaati> mevcutSaatler = calismaSaatiRepository.findByUzmanId(hedefUzman.getId());
        calismaSaatiRepository.deleteAll(mevcutSaatler);

        List<CalismaSaati> yeniSaatler = requestList.stream().map(req -> CalismaSaati.builder()
                .uzman(hedefUzman)
                .gunOfWeek(req.getGunOfWeek())
                .baslangicSaat(req.getBaslangicSaat())
                .bitisSaat(req.getBitisSaat())
                .kapaliMi(req.getKapaliMi())
                .build()).toList();

        return calismaSaatiRepository.saveAll(yeniSaatler);
    }
}
