package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.UserCreateRequest;
import com.appointflow.dto.UserResponse;
import com.appointflow.dto.UserUpdateRequest;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.entity.Sube;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.SubeRepository;
import com.appointflow.subscription.service.QuotaService;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KullaniciRepository kullaniciRepository;
    private final SubeRepository subeRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuotaService quotaService;
    private final com.appointflow.audit.service.AuditService auditService;
    private final EmailVerificationService emailVerificationService;
    private final com.appointflow.repository.StaffServiceRepository staffServiceRepository;
    private final com.appointflow.repository.HizmetRepository hizmetRepository;

    public List<UserResponse> getAll() {
        Long tenantId = TenantContext.getTenantId();
        return kullaniciRepository.findByTenantId(tenantId).stream()
                .filter(k -> k.getRol() != Role.MUSTERI && k.getRol() != Role.SUPER_ADMIN) // Musteri ve super admin haric tut
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getById(Long id) {
        Kullanici k = findByIdAndTenant(id);
        return toResponse(k);
    }

    public UserResponse create(UserCreateRequest request) {
        if (kullaniciRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Bu email zaten kayitli.");
        }

        Role role = parseRole(request.getRol());

        Long tenantId = TenantContext.getTenantId();
        if (role == Role.STAFF || role == Role.BRANCH_MANAGER) {
            quotaService.assertStaffQuota(tenantId);
        }

        Kullanici kullanici = Kullanici.builder()
                .tenantId(tenantId)
                .ad(request.getAd())
                .soyad(request.getSoyad())
                .email(request.getEmail())
                .sifre(passwordEncoder.encode(request.getSifre()))
                .telefon(request.getTelefon())
                .rol(role)
                .build();

        if (request.getSubeId() != null) {
            Sube sube = subeRepository.findById(request.getSubeId())
                    .orElseThrow(() -> ApiException.notFound("Sube bulunamadi."));
            kullanici.setSube(sube);
        }

        if (request.getPozisyon() != null && !request.getPozisyon().isBlank()) {
            kullanici.setPozisyon(parsePozisyon(request.getPozisyon()));
        }

        kullaniciRepository.save(kullanici);

        // Çalışanın yapabileceği hizmetleri ata (varsa)
        if (request.getHizmetIds() != null && !request.getHizmetIds().isEmpty()) {
            syncStaffServices(kullanici.getId(), request.getHizmetIds());
        }

        // Calisana dogrulama maili gonder — login engellememesi icin emailDogrulandi=false default kalir
        try {
            emailVerificationService.sendVerification(kullanici);
            log.info("Yeni calisan icin dogrulama maili gonderildi: email={}, kullaniciId={}",
                    kullanici.getEmail(), kullanici.getId());
        } catch (Exception e) {
            // Mail gonderim hatasi calisan olusturmayi bozmaz — ama loglansin
            log.error("Calisan {} icin dogrulama maili gonderilemedi: {}",
                    kullanici.getEmail(), e.getMessage(), e);
        }

        auditService.log("USER_CREATE", "Kullanici", kullanici.getId(),
                java.util.Map.of("email", kullanici.getEmail(), "rol", role.name()));
        return toResponse(kullanici);
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        Kullanici k = findByIdAndTenant(id);
        k.setAd(request.getAd());
        k.setSoyad(request.getSoyad());
        k.setTelefon(request.getTelefon());

        if (request.getRol() != null) {
            Role yeniRol = parseRole(request.getRol());
            Role eskiRol = k.getRol();

            // Guard 1: Tek OWNER kuralı — son OWNER'ı başka role düşürme
            if (eskiRol == Role.OWNER && yeniRol != Role.OWNER) {
                long ownerCount = kullaniciRepository.countOwnersByTenantId(k.getTenantId());
                if (ownerCount <= 1) {
                    throw ApiException.badRequest(
                            "Bu işletmedeki tek sahip (OWNER) rolünü değiştiremezsiniz. Önce başka bir OWNER atayın.");
                }
            }

            // Guard 2: OWNER'a yükseltme — başka OWNER varsa engellensin (SUPER_ADMIN harici)
            if (eskiRol != Role.OWNER && yeniRol == Role.OWNER) {
                throw ApiException.forbidden(
                        "OWNER rolünü doğrudan atayamazsınız. Tenant sahipliği devri için SUPER_ADMIN ile iletişime geçin.");
            }

            k.setRol(yeniRol);
        }

        if (request.getSubeId() != null) {
            Sube sube = subeRepository.findById(request.getSubeId())
                    .orElseThrow(() -> ApiException.notFound("Sube bulunamadi."));
            k.setSube(sube);
        }

        if (request.getPozisyon() != null) {
            k.setPozisyon(request.getPozisyon().isBlank() ? null : parsePozisyon(request.getPozisyon()));
        }

        kullaniciRepository.save(k);

        // hizmetIds null → değişiklik yok; boş liste → tüm atamalar silinir; dolu → set
        if (request.getHizmetIds() != null) {
            syncStaffServices(k.getId(), request.getHizmetIds());
        }

        auditService.log("USER_UPDATE", "Kullanici", k.getId(),
                java.util.Map.of("email", k.getEmail()));
        return toResponse(k);
    }

    /**
     * Çalışanın yapabileceği hizmetleri set eder (önce hepsini siler, sonra yeni listeyi ekler).
     */
    @org.springframework.transaction.annotation.Transactional
    protected void syncStaffServices(Long kullaniciId, java.util.List<Long> hizmetIds) {
        Long tenantId = TenantContext.getTenantId();
        staffServiceRepository.deleteByKullaniciId(kullaniciId);
        for (Long hid : hizmetIds) {
            var hizmet = hizmetRepository.findById(hid).orElse(null);
            if (hizmet == null || !hizmet.getTenantId().equals(tenantId)) continue;
            var k = kullaniciRepository.findById(kullaniciId).orElse(null);
            if (k == null) continue;
            staffServiceRepository.save(
                    com.appointflow.entity.StaffService.builder()
                            .kullanici(k)
                            .hizmet(hizmet)
                            .build());
        }
    }

    public void delete(Long id) {
        Kullanici k = findByIdAndTenant(id);
        k.setAktif(false);
        kullaniciRepository.save(k);
        auditService.log("USER_DELETE", "Kullanici", k.getId(),
                java.util.Map.of("email", k.getEmail()));
    }

    /**
     * Owner çalışana yeniden doğrulama maili göndertir.
     */
    public void resendVerificationForStaff(Long id) {
        Kullanici k = findByIdAndTenant(id);
        if (Boolean.TRUE.equals(k.getEmailDogrulandi())) {
            throw ApiException.badRequest("Bu kullanicinin emaili zaten dogrulanmis.");
        }
        emailVerificationService.sendVerification(k);
        auditService.log("USER_RESEND_VERIFICATION", "Kullanici", k.getId(),
                java.util.Map.of("email", k.getEmail()));
    }

    /**
     * Owner çalışanın emailini manuel doğrulanmış işaretler (mail erişimi yoksa).
     */
    public UserResponse markEmailVerified(Long id) {
        Kullanici k = findByIdAndTenant(id);
        k.setEmailDogrulandi(true);
        kullaniciRepository.save(k);
        auditService.log("USER_EMAIL_MARK_VERIFIED", "Kullanici", k.getId(),
                java.util.Map.of("email", k.getEmail()));
        return toResponse(k);
    }

    public UserResponse activate(Long id) {
        Kullanici k = findByIdAndTenant(id);
        if (Boolean.TRUE.equals(k.getAktif())) {
            return toResponse(k);
        }
        // Pasif → aktif olurken staff quota'sını tekrar kontrol et
        if (k.getRol() == Role.STAFF || k.getRol() == Role.BRANCH_MANAGER) {
            quotaService.assertStaffQuota(TenantContext.getTenantId());
        }
        k.setAktif(true);
        kullaniciRepository.save(k);
        auditService.log("USER_ACTIVATE", "Kullanici", k.getId(),
                java.util.Map.of("email", k.getEmail()));
        return toResponse(k);
    }

    private Kullanici findByIdAndTenant(Long id) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        if (!k.getTenantId().equals(TenantContext.getTenantId())) {
            throw ApiException.forbidden("Bu kullaniciya erisim yetkiniz yok.");
        }
        return k;
    }

    private com.appointflow.tenant.StaffPosition parsePozisyon(String value) {
        try {
            return com.appointflow.tenant.StaffPosition.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz pozisyon: " + value);
        }
    }

    private Role parseRole(String rolStr) {
        try {
            Role role = Role.valueOf(rolStr.toUpperCase());
            if (role == Role.MUSTERI) {
                throw ApiException.badRequest("MUSTERI rolu calisan olusturmada kullanilamaz.");
            }
            if (role == Role.SUPER_ADMIN) {
                throw ApiException.forbidden("SUPER_ADMIN rolu atanamaz.");
            }
            return role;
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Gecersiz rol: " + rolStr);
        }
    }

    private UserResponse toResponse(Kullanici k) {
        return UserResponse.builder()
                .id(k.getId())
                .ad(k.getAd())
                .soyad(k.getSoyad())
                .email(k.getEmail())
                .telefon(k.getTelefon())
                .rol(k.getRol().name())
                .subeId(k.getSube() != null ? k.getSube().getId() : null)
                .subeAd(k.getSube() != null ? k.getSube().getAd() : null)
                .pozisyon(k.getPozisyon() != null ? k.getPozisyon().name() : null)
                .pozisyonAd(k.getPozisyon() != null ? k.getPozisyon().displayName() : null)
                .aktif(k.getAktif())
                .emailDogrulandi(k.getEmailDogrulandi())
                .hizmetIds(staffServiceRepository.findByKullaniciId(k.getId()).stream()
                        .map(s -> s.getHizmet().getId())
                        .collect(java.util.stream.Collectors.toList()))
                .sonGirisTarihi(k.getSonGirisTarihi())
                .createdAt(k.getCreatedAt())
                .build();
    }
}
