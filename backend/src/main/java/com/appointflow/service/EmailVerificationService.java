package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.entity.EmailVerificationToken;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.EmailVerificationTokenRepository;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final KullaniciRepository kullaniciRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final MailService mailService;

    @Value("${app.email-verification.ttl-hours:48}")
    private int ttlHours;

    @Value("${app.email-verification.url:http://localhost:3000/verify-email?token=}")
    private String verifyUrlBase;

    @Transactional
    public void sendVerification(Kullanici kullanici) {
        if (Boolean.TRUE.equals(kullanici.getEmailDogrulandi())) {
            return;
        }
        tokenRepository.deleteByKullaniciId(kullanici.getId());

        String token = generateToken();
        EmailVerificationToken entity = EmailVerificationToken.builder()
                .kullaniciId(kullanici.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                .build();
        tokenRepository.save(entity);

        String body = String.format(
                "Merhaba %s,%n%n" +
                "AppointFlow hesabinizi dogrulamak icin asagidaki baglantiyi tiklayin (%d saat gecerlidir):%n%n" +
                "%s%s%n%n" +
                "Bu istegi siz yapmadiysaniz bu mesaji yok sayabilirsiniz.%n",
                kullanici.getAd(), ttlHours, verifyUrlBase, token);

        mailService.send(kullanici.getEmail(), "AppointFlow - E-posta adresinizi dogrulayin", body);
    }

    @Transactional
    public void resendVerification(String email) {
        Kullanici kullanici = kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        if (Boolean.TRUE.equals(kullanici.getEmailDogrulandi())) {
            throw ApiException.badRequest("E-postaniz zaten dogrulanmis.");
        }
        sendVerification(kullanici);
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken entity = tokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("Token gecersiz."));
        if (!entity.isValid()) {
            throw ApiException.badRequest("Token suresi dolmus veya zaten kullanilmis.");
        }
        Kullanici kullanici = kullaniciRepository.findById(entity.getKullaniciId())
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        kullanici.setEmailDogrulandi(true);
        kullaniciRepository.save(kullanici);
        entity.setUsedAt(LocalDateTime.now());
        tokenRepository.save(entity);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
