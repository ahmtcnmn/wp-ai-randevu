package com.appointflow.contact.service;

import com.appointflow.common.ApiException;
import com.appointflow.contact.dto.ContactRequestResponse;
import com.appointflow.contact.dto.ContactRequestSubmission;
import com.appointflow.contact.entity.ContactRequest;
import com.appointflow.contact.repository.ContactRequestRepository;
import com.appointflow.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository repository;
    private final MailService mailService;

    @Value("${app.contact.admin-email:info@ehasoftware.com}")
    private String adminEmail;

    /**
     * Public iletisim formu submit'i — herkes cagirabilir.
     * Spam korumasi: ip-bazli basit rate limit (ileride eklenebilir).
     */
    @Transactional
    public ContactRequest submit(ContactRequestSubmission body, HttpServletRequest req) {
        ContactRequest entry = ContactRequest.builder()
                .ad(body.getAd().trim())
                .soyad(body.getSoyad().trim())
                .email(body.getEmail().trim().toLowerCase())
                .telefon(body.getTelefon().trim())
                .mesaj(body.getMesaj().trim())
                .ipAddress(resolveIp(req))
                .userAgent(req != null ? req.getHeader("User-Agent") : null)
                .durum("YENI")
                .build();

        ContactRequest saved = repository.save(entry);
        log.info("Contact request alindi: id={}, email={}, ip={}",
                saved.getId(), saved.getEmail(), saved.getIpAddress());

        // Admin'e bilgi maili (mail aktif degilse log'a duser)
        try {
            String body2 = String.format(
                    "Yeni iletisim talebi:%n%n" +
                    "Ad Soyad: %s %s%nE-posta: %s%nTelefon: %s%n%nMesaj:%n%s%n%n" +
                    "IP: %s%nUA: %s%nTarih: %s",
                    saved.getAd(), saved.getSoyad(), saved.getEmail(), saved.getTelefon(),
                    saved.getMesaj(),
                    saved.getIpAddress(),
                    saved.getUserAgent(),
                    saved.getCreatedAt());
            mailService.send(adminEmail, "AppointFlow - Yeni iletisim talebi #" + saved.getId(), body2);
        } catch (Exception e) {
            log.warn("Admin bilgilendirme maili gonderilemedi: {}", e.getMessage());
        }

        return saved;
    }

    /** SUPER_ADMIN sayfasi icin liste */
    public Page<ContactRequestResponse> list(String durum, Pageable pageable) {
        Page<ContactRequest> page = (durum != null && !durum.isBlank())
                ? repository.findByDurumOrderByCreatedAtDesc(durum.toUpperCase(), pageable)
                : repository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public ContactRequestResponse updateStatus(Long id, String durum, String not, Long cevaplayanUserId) {
        ContactRequest entry = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Talep bulunamadi."));
        entry.setDurum(durum.toUpperCase());
        if (not != null) entry.setSuperAdminNotu(not);
        if (cevaplayanUserId != null) {
            entry.setCevaplayanUserId(cevaplayanUserId);
            entry.setCevaplanmaTarihi(LocalDateTime.now());
        }
        return toResponse(repository.save(entry));
    }

    private String resolveIp(HttpServletRequest req) {
        if (req == null) return null;
        String xfwd = req.getHeader("X-Forwarded-For");
        if (xfwd != null && !xfwd.isBlank()) {
            return xfwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private ContactRequestResponse toResponse(ContactRequest c) {
        return ContactRequestResponse.builder()
                .id(c.getId())
                .ad(c.getAd())
                .soyad(c.getSoyad())
                .email(c.getEmail())
                .telefon(c.getTelefon())
                .mesaj(c.getMesaj())
                .ipAddress(c.getIpAddress())
                .userAgent(c.getUserAgent())
                .durum(c.getDurum())
                .superAdminNotu(c.getSuperAdminNotu())
                .cevaplayanUserId(c.getCevaplayanUserId())
                .cevaplanmaTarihi(c.getCevaplanmaTarihi())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
