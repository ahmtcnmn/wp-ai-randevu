package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.*;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.Role;
import com.appointflow.entity.Tenant;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.TenantRepository;
import com.appointflow.security.JwtUtil;
import com.appointflow.subscription.entity.Subscription;
import com.appointflow.subscription.entity.SubscriptionPlan;
import com.appointflow.subscription.entity.SubscriptionStatus;
import com.appointflow.subscription.repository.SubscriptionPlanRepository;
import com.appointflow.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KullaniciRepository kullaniciRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailVerificationService emailVerificationService;
    private final com.appointflow.audit.service.AuditService auditService;
    private final TwoFactorService twoFactorService;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String TWOFA_TEMP_PREFIX = "2fa_temp:";
    private static final long TWOFA_TEMP_TTL_SECONDS = 300; // 5 dakika

    @Transactional
    public AuthResponse kayitOl(KayitRequest request) {
        if (kullaniciRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Bu email zaten kayitli.");
        }

        // Her kayıt için yeni bir tenant oluştur
        String slug = generateSlug(request.getAd(), request.getSoyad(), request.getEmail());
        String tenantAd = request.getIsletmeAdi() != null && !request.getIsletmeAdi().isBlank()
                ? request.getIsletmeAdi()
                : request.getAd() + " " + request.getSoyad();
        com.appointflow.tenant.BusinessType businessType = request.getBusinessType() != null
                ? request.getBusinessType()
                : com.appointflow.tenant.BusinessType.OTHER;
        Tenant tenant = Tenant.builder()
                .ad(tenantAd)
                .slug(slug)
                .email(request.getEmail())
                .telefon(request.getTelefon())
                .businessType(businessType)
                .build();
        final Tenant savedTenant = tenantRepository.save(tenant);

        Kullanici kullanici = Kullanici.builder()
                .ad(request.getAd())
                .soyad(request.getSoyad())
                .email(request.getEmail())
                .sifre(passwordEncoder.encode(request.getSifre()))
                .telefon(request.getTelefon())
                .rol(Role.OWNER)
                .tenantId(savedTenant.getId())
                .emailDogrulandi(false)
                .build();
        kullaniciRepository.save(kullanici);
        emailVerificationService.sendVerification(kullanici);

        // Yeni tenant'a GROWTH planıyla 14 günlük TRIALING abonelik oluştur (kart bilgisi alınmaz)
        // Trial bitince SubscriptionExpiryJob bu aboneliği EXPIRED'a çevirir,
        // kullanıcı /finans'a yönlendirilip plan seçmeye zorlanır.
        SubscriptionPlan growthPlan = subscriptionPlanRepository.findByPlanKey("GROWTH")
                .orElseThrow(() -> {
                    log.error("GROWTH plani veritabaninda bulunamadi. Yeni kullanici kayit alinamaz.");
                    return ApiException.internalError(
                            "Abonelik sistemi yapilandirilmamis. Lutfen sistem yoneticisi ile iletisime gecin.");
                });
        Subscription subscription = Subscription.builder()
                .tenantId(savedTenant.getId())
                .plan(growthPlan)
                .status(SubscriptionStatus.TRIALING)
                .baslangicTarihi(LocalDateTime.now())
                .denemeBitisTarihi(LocalDateTime.now().plusDays(14))
                .build();
        subscriptionRepository.save(subscription);

        UserDetails userDetails = userDetailsService.loadUserByUsername(kullanici.getEmail());
        String token = jwtUtil.generateToken(userDetails, kullanici.getTenantId(), kullanici.getId());
        String refreshToken = jwtUtil.generateRefreshToken(kullanici.getEmail(), kullanici.getTenantId());

        storeRefreshToken(kullanici.getEmail(), refreshToken);

        auditService.log("REGISTER", "Kullanici", kullanici.getId(),
                java.util.Map.of("email", kullanici.getEmail(), "tenantId", savedTenant.getId()));

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(kullanici.getEmail())
                .ad(kullanici.getAd())
                .soyad(kullanici.getSoyad())
                .rol(kullanici.getRol().name())
                .tenantId(kullanici.getTenantId())
                .build();
    }

    public AuthResponse girisYap(GirisRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSifre())
        );
        Kullanici kullanici = kullaniciRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        if (!kullanici.getAktif()) {
            throw ApiException.forbidden("Hesabiniz devre disi birakilmis.");
        }
        if (kullanici.getDeletedAt() != null) {
            throw ApiException.forbidden("Hesabiniz silinmek uzere isaretli. Geri yuklemek icin destek ile iletisime gecin.");
        }

        // Email dogrulanmis mi? (SUPER_ADMIN bypass)
        if (kullanici.getRol() != Role.SUPER_ADMIN
                && !Boolean.TRUE.equals(kullanici.getEmailDogrulandi())) {
            auditService.log("LOGIN_BLOCKED_EMAIL_UNVERIFIED", "Kullanici", kullanici.getId(),
                    java.util.Map.of("email", kullanici.getEmail()));
            throw new ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "E-postanizi dogrulamadan giris yapamazsiniz. Lutfen mail kutunuzu kontrol edin.",
                    "EMAIL_NOT_VERIFIED");
        }

        // Tenant aktif mi kontrolü + sektör eşleşmesi (SUPER_ADMIN için bypass)
        if (kullanici.getRol() != Role.SUPER_ADMIN && kullanici.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(kullanici.getTenantId()).orElse(null);
            if (tenant != null) {
                if (!tenant.getAktif()) {
                    throw ApiException.forbidden("Bu isletme hesabi devre disi birakilmis. Destek icin platform yoneticisiyle iletisime gecin.");
                }
                // Mobil app build-time sektör kontrolü — sadece istek body'de expectedBusinessType gönderildiyse
                if (request.getExpectedBusinessType() != null
                        && tenant.getBusinessType() != null
                        && tenant.getBusinessType() != request.getExpectedBusinessType()) {
                    String expectedName = com.appointflow.tenant.SectorLabels.getDisplayName(tenant.getBusinessType());
                    auditService.log("LOGIN_WRONG_APP", "Kullanici", kullanici.getId(),
                            java.util.Map.of(
                                    "tenantType", tenant.getBusinessType().name(),
                                    "expectedType", request.getExpectedBusinessType().name()));
                    throw new ApiException(
                            org.springframework.http.HttpStatus.FORBIDDEN,
                            "Bu hesap " + expectedName + " icin. Lutfen dogru uygulamayi indirin.",
                            "WRONG_APP_FOR_BUSINESS_TYPE");
                }
            }
        }

        // 2FA aktifse — temp token uret, gercek JWT verme
        if (Boolean.TRUE.equals(kullanici.getTwoFactorEnabled())) {
            String tempToken = java.util.UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(TWOFA_TEMP_PREFIX + tempToken,
                    kullanici.getEmail(), TWOFA_TEMP_TTL_SECONDS, TimeUnit.SECONDS);

            auditService.log("LOGIN_2FA_REQUIRED", "Kullanici", kullanici.getId(),
                    java.util.Map.of("email", kullanici.getEmail()));

            return AuthResponse.builder()
                    .requires2fa(true)
                    .tempToken(tempToken)
                    .email(kullanici.getEmail())
                    .build();
        }

        // Son giris tarihini guncelle
        kullanici.setSonGirisTarihi(LocalDateTime.now());
        kullaniciRepository.save(kullanici);

        UserDetails userDetails = userDetailsService.loadUserByUsername(kullanici.getEmail());
        String token = jwtUtil.generateToken(userDetails, kullanici.getTenantId(), kullanici.getId());
        String refreshToken = jwtUtil.generateRefreshToken(kullanici.getEmail(), kullanici.getTenantId());

        storeRefreshToken(kullanici.getEmail(), refreshToken);

        auditService.log("LOGIN", "Kullanici", kullanici.getId(),
                java.util.Map.of("email", kullanici.getEmail()));

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(kullanici.getEmail())
                .ad(kullanici.getAd())
                .soyad(kullanici.getSoyad())
                .rol(kullanici.getRol().name())
                .tenantId(kullanici.getTenantId())
                .build();
    }

    /**
     * 2FA login akisinin 2. adimi — tempToken + code ile gercek JWT donmek.
     */
    public AuthResponse verifyTwoFactorLogin(String tempToken, String code) {
        String email = redisTemplate.opsForValue().get(TWOFA_TEMP_PREFIX + tempToken);
        if (email == null) {
            throw ApiException.unauthorized("Temp token gecersiz veya suresi dolmus.");
        }
        Kullanici kullanici = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        if (!Boolean.TRUE.equals(kullanici.getTwoFactorEnabled())) {
            throw ApiException.badRequest("2FA aktif degil.");
        }

        boolean ok = twoFactorService.verifyLoginCode(
                kullanici.getId(), kullanici.getTwoFactorSecret(), code);
        if (!ok) {
            auditService.log("LOGIN_2FA_FAILED", "Kullanici", kullanici.getId(), null);
            throw ApiException.badRequest("Dogrulama kodu hatali veya kullanilmis.");
        }

        // Temp token'i tuket
        redisTemplate.delete(TWOFA_TEMP_PREFIX + tempToken);

        kullanici.setSonGirisTarihi(LocalDateTime.now());
        kullaniciRepository.save(kullanici);

        UserDetails userDetails = userDetailsService.loadUserByUsername(kullanici.getEmail());
        String token = jwtUtil.generateToken(userDetails, kullanici.getTenantId(), kullanici.getId());
        String refreshToken = jwtUtil.generateRefreshToken(kullanici.getEmail(), kullanici.getTenantId());
        storeRefreshToken(kullanici.getEmail(), refreshToken);

        auditService.log("LOGIN", "Kullanici", kullanici.getId(),
                java.util.Map.of("email", kullanici.getEmail(), "twoFactor", true));

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(kullanici.getEmail())
                .ad(kullanici.getAd())
                .soyad(kullanici.getSoyad())
                .rol(kullanici.getRol().name())
                .tenantId(kullanici.getTenantId())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String email;
        try {
            email = jwtUtil.extractEmail(refreshToken);
        } catch (Exception e) {
            throw ApiException.unauthorized("Gecersiz veya bozuk refresh token.");
        }

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw ApiException.badRequest("Gecersiz refresh token.");
        }

        // Redis'ten dogrula
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw ApiException.unauthorized("Refresh token gecersiz veya suresi dolmus.");
        }

        Kullanici kullanici = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String newToken = jwtUtil.generateToken(userDetails, kullanici.getTenantId(), kullanici.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(email, kullanici.getTenantId());

        // Eski refresh token'i sil, yenisini kaydet
        storeRefreshToken(email, newRefreshToken);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .email(kullanici.getEmail())
                .ad(kullanici.getAd())
                .soyad(kullanici.getSoyad())
                .rol(kullanici.getRol().name())
                .tenantId(kullanici.getTenantId())
                .build();
    }

    public void logout(String email) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
        auditService.log("LOGOUT", java.util.Map.of("email", email));
    }

    public UserResponse getProfile(String email) {
        Kullanici k = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        return toUserResponse(k);
    }

    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        Kullanici k = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        k.setAd(request.getAd());
        k.setSoyad(request.getSoyad());
        k.setTelefon(request.getTelefon());
        kullaniciRepository.save(k);
        return toUserResponse(k);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        Kullanici k = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        if (!passwordEncoder.matches(request.getMevcutSifre(), k.getSifre())) {
            throw ApiException.badRequest("Mevcut sifre yanlis.");
        }
        k.setSifre(passwordEncoder.encode(request.getYeniSifre()));
        kullaniciRepository.save(k);
        auditService.log("PASSWORD_CHANGE", "Kullanici", k.getId(), null);
    }

    private void storeRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + email,
                refreshToken,
                jwtUtil.getRefreshExpirationMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private String generateSlug(String ad, String soyad, String email) {
        // email'in @ öncesi + rastgele suffix ile benzersiz slug üret
        String base = email.split("@")[0]
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-");
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String candidate = base + "-" + suffix;
        // Çakışma varsa tekrar dene
        int attempts = 0;
        while (tenantRepository.existsBySlug(candidate) && attempts < 10) {
            suffix = String.valueOf((System.currentTimeMillis() + attempts) % 100000);
            candidate = base + "-" + suffix;
            attempts++;
        }
        return candidate;
    }

    private UserResponse toUserResponse(Kullanici k) {
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
                .sonGirisTarihi(k.getSonGirisTarihi())
                .createdAt(k.getCreatedAt())
                .build();
    }
}
