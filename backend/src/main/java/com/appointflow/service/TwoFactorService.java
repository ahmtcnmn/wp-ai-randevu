package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.TwoFactorEnableResponse;
import com.appointflow.dto.TwoFactorSetupResponse;
import com.appointflow.entity.Kullanici;
import com.appointflow.entity.TwoFactorRecoveryCode;
import com.appointflow.repository.KullaniciRepository;
import com.appointflow.repository.TwoFactorRecoveryCodeRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RECOVERY_CODE_COUNT = 10;

    private final KullaniciRepository kullaniciRepository;
    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.appointflow.audit.service.AuditService auditService;

    @Value("${app.two-factor.issuer:AppointFlow}")
    private String issuer;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    /**
     * 1) Kullanici 2FA setup baslatir — pending_secret olusur, QR donulur.
     * Henuz aktif degil — kullanici "verify" cagrisi ile aktive eder.
     */
    @Transactional
    public TwoFactorSetupResponse beginSetup(String email) {
        Kullanici user = findUser(email);
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw ApiException.badRequest("2FA zaten aktif.");
        }

        String secret = secretGenerator.generate();
        user.setTwoFactorPendingSecret(secret);
        kullaniciRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrDataUri;
        try {
            qrDataUri = Utils.getDataUriForImage(
                    qrGenerator.generate(qrData),
                    qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("QR generate hatasi: {}", e.getMessage());
            throw ApiException.internalError("QR kod uretilemedi.");
        }

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeDataUri(qrDataUri)
                .otpauthUrl(qrData.getUri())
                .build();
    }

    /**
     * 2) Kullanici authenticator'dan kod alip dogrular — aktif eder, recovery code uretir.
     */
    @Transactional
    public TwoFactorEnableResponse verifyAndEnable(String email, String code) {
        Kullanici user = findUser(email);
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw ApiException.badRequest("2FA zaten aktif.");
        }
        String pending = user.getTwoFactorPendingSecret();
        if (pending == null) {
            throw ApiException.badRequest("Once 2FA kurulumu baslatilmali.");
        }
        if (!codeVerifier.isValidCode(pending, code)) {
            throw ApiException.badRequest("Dogrulama kodu hatali.");
        }

        user.setTwoFactorSecret(pending);
        user.setTwoFactorPendingSecret(null);
        user.setTwoFactorEnabled(true);
        kullaniciRepository.save(user);

        // Recovery codes uret
        recoveryCodeRepository.deleteByKullaniciId(user.getId());
        List<String> plainCodes = generateRecoveryCodes(RECOVERY_CODE_COUNT);
        for (String plain : plainCodes) {
            recoveryCodeRepository.save(TwoFactorRecoveryCode.builder()
                    .kullaniciId(user.getId())
                    .codeHash(hash(plain))
                    .build());
        }

        auditService.log("TWOFACTOR_ENABLE", "Kullanici", user.getId(), null);
        return new TwoFactorEnableResponse(plainCodes);
    }

    /**
     * 3) Kullanici 2FA'yi kapatir (sifre dogrulama gerekir).
     */
    @Transactional
    public void disable(String email, String sifre) {
        Kullanici user = findUser(email);
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw ApiException.badRequest("2FA zaten kapali.");
        }
        if (!passwordEncoder.matches(sifre, user.getSifre())) {
            throw ApiException.badRequest("Sifre yanlis.");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorPendingSecret(null);
        kullaniciRepository.save(user);
        recoveryCodeRepository.deleteByKullaniciId(user.getId());

        auditService.log("TWOFACTOR_DISABLE", "Kullanici", user.getId(), null);
    }

    /**
     * Login akisinda kullanilir — TOTP kodu veya recovery code dogrulanir.
     */
    public boolean verifyLoginCode(Long userId, String secret, String code) {
        if (code == null || code.isBlank()) return false;

        // Once normal TOTP kodu
        if (codeVerifier.isValidCode(secret, code)) {
            return true;
        }

        // Recovery code kontrolu
        String trimmed = code.replaceAll("[-\\s]", "").toUpperCase();
        var rcOpt = recoveryCodeRepository.findByCodeHashAndUsedAtIsNull(hash(trimmed));
        if (rcOpt.isPresent() && rcOpt.get().getKullaniciId().equals(userId)) {
            TwoFactorRecoveryCode rc = rcOpt.get();
            rc.setUsedAt(java.time.LocalDateTime.now());
            recoveryCodeRepository.save(rc);
            auditService.log("TWOFACTOR_RECOVERY_USED", "Kullanici", userId, null);
            return true;
        }
        return false;
    }

    public long countUnusedRecoveryCodes(Long userId) {
        return recoveryCodeRepository.countByKullaniciIdAndUsedAtIsNull(userId);
    }

    public long countUnusedRecoveryCodesByEmail(String email) {
        Kullanici user = findUser(email);
        return recoveryCodeRepository.countByKullaniciIdAndUsedAtIsNull(user.getId());
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(String email) {
        Kullanici user = findUser(email);
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw ApiException.badRequest("2FA aktif degil.");
        }
        recoveryCodeRepository.deleteByKullaniciId(user.getId());
        List<String> plainCodes = generateRecoveryCodes(RECOVERY_CODE_COUNT);
        for (String plain : plainCodes) {
            recoveryCodeRepository.save(TwoFactorRecoveryCode.builder()
                    .kullaniciId(user.getId())
                    .codeHash(hash(plain))
                    .build());
        }
        auditService.log("TWOFACTOR_RECOVERY_REGENERATE", "Kullanici", user.getId(), null);
        return plainCodes;
    }

    private Kullanici findUser(String email) {
        return kullaniciRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
    }

    private List<String> generateRecoveryCodes(int count) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 10 alphanumeric karakter, ortada tire — gorsel okuma
            byte[] bytes = new byte[6];
            RANDOM.nextBytes(bytes);
            String hex = HexFormat.of().formatHex(bytes).toUpperCase();
            out.add(hex.substring(0, 5) + "-" + hex.substring(5, 10));
        }
        return out;
    }

    private String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.replaceAll("[-\\s]", "").toUpperCase().getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Hash uretilemedi", e);
        }
    }
}
