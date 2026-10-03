package com.appointflow.whatsapp.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
public class SignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * HMAC-SHA256 ile imza dogrulama.
     * Timing-safe karsilastirma kullanir (timing attack koruması).
     */
    public boolean verify(String appSecret, String payload, String signatureHeader) {
        if (appSecret == null || payload == null || signatureHeader == null) {
            return false;
        }

        try {
            // "sha256=..." formatindaki signature'dan hash'i al
            String expectedPrefix = "sha256=";
            if (!signatureHeader.startsWith(expectedPrefix)) {
                log.warn("Gecersiz signature formati — beklenen: sha256=...");
                return false;
            }
            String receivedHash = signatureHeader.substring(expectedPrefix.length());

            // HMAC-SHA256 hesapla
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Hex formatina cevir
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            String computedHash = sb.toString();

            // Timing-safe karsilastirma
            return MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    receivedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature dogrulama hatasi: {}", e.getMessage());
            return false;
        }
    }
}
