package com.appointflow.whatsapp.service;

import com.appointflow.whatsapp.entity.WhatsappConfig;
import com.appointflow.whatsapp.repository.WhatsappConfigRepository;
import com.appointflow.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappMessageService {

    private static final String GRAPH_API_URL = "https://graph.facebook.com/v21.0";

    private final WhatsappConfigRepository configRepository;
    private final WebClient.Builder webClientBuilder;

    /**
     * Serbest metin mesaji gonder (24h pencere icinde).
     */
    public void sendTextMessage(Long tenantId, String to, String text) {
        WhatsappConfig config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("WhatsApp yapilandirmasi bulunamadi"));

        if (!config.getAktif() || config.getAccessToken() == null) {
            log.warn("WhatsApp aktif degil veya token yok — tenant: {}", tenantId);
            return;
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        sendRequest(config, body);
        log.info("WhatsApp mesaj gonderildi — to: {}, tenant: {}", maskPhone(to), tenantId);
    }

    /**
     * Template mesaji gonder (24h pencere disinda da calisir).
     */
    public void sendTemplateMessage(Long tenantId, String to, String templateName, String languageCode) {
        WhatsappConfig config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("WhatsApp yapilandirmasi bulunamadi"));

        if (!config.getAktif() || config.getAccessToken() == null) {
            log.warn("WhatsApp aktif degil veya token yok — tenant: {}", tenantId);
            return;
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", languageCode != null ? languageCode : "tr")
                )
        );

        sendRequest(config, body);
        log.info("WhatsApp template mesaj gonderildi — to: {}, template: {}", maskPhone(to), templateName);
    }

    private void sendRequest(WhatsappConfig config, Map<String, Object> body) {
        String url = GRAPH_API_URL + "/" + config.getPhoneNumberId() + "/messages";

        try {
            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("WhatsApp mesaj gonderilemedi: {}", e.getMessage());
            throw new RuntimeException("WhatsApp mesaj gonderilemedi", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 3);
    }
}
