package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.ForgotPasswordRequest;
import com.appointflow.dto.ResetPasswordRequest;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.PasswordResetToken;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KullaniciRepository kullaniciRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.password-reset.ttl-minutes:60}")
    private int ttlMinutes;

    @Value("${app.password-reset.url:http://localhost:3000/reset-password?token=}")
    private String resetUrlBase;

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        // Timing-safe: kullanici varsa veya yoksa hep ayni davran.
        kullaniciRepository.findByEmail(request.getEmail()).ifPresent(this::issueToken);
    }

    private void issueToken(Kullanici kullanici) {
        tokenRepository.deleteByKullaniciId(kullanici.getId());

        String token = generateToken();
        PasswordResetToken entity = PasswordResetToken.builder()
                .kullaniciId(kullanici.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .build();
        tokenRepository.save(entity);

        String body = String.format(
                "Merhaba %s,%n%n" +
                "Sifrenizi sifirlamak icin asagidaki baglantiyi kullanin (%d dakika gecerlidir):%n%n" +
                "%s%s%n%n" +
                "Bu istegi siz yapmadiysaniz bu mesaji yok sayabilirsiniz.%n",
                kullanici.getAd(), ttlMinutes, resetUrlBase, token);

        mailService.send(kullanici.getEmail(), "AppointFlow - Sifre sifirlama talebi", body);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Token gecersiz."));

        if (!token.isValid()) {
            throw ApiException.badRequest("Token suresi dolmus veya zaten kullanilmis.");
        }

        Kullanici kullanici = kullaniciRepository.findById(token.getKullaniciId())
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        kullanici.setSifre(passwordEncoder.encode(request.getYeniSifre()));
        kullaniciRepository.save(kullanici);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        // Eski refresh token'i da gecersiz kil
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + kullanici.getEmail());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
