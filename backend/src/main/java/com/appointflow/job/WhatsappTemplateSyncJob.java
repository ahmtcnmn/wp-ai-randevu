package com.appointflow.job;

import com.appointflow.whatsapp.entity.WhatsappConfig;
import com.appointflow.whatsapp.entity.WhatsappTemplate;
import com.appointflow.whatsapp.repository.WhatsappConfigRepository;
import com.appointflow.whatsapp.repository.WhatsappTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsappTemplateSyncJob {

    private static final String GRAPH_API_URL = "https://graph.facebook.com/v21.0";

    private final WhatsappConfigRepository configRepository;
    private final WhatsappTemplateRepository templateRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 */6 * * *")
    public void syncTemplates() {
        log.info("WhatsApp template sync job'ı başlıyor...");
        List<WhatsappConfig> configs = configRepository.findAll();

        for (WhatsappConfig config : configs) {
            if (!Boolean.TRUE.equals(config.getAktif()) || config.getAccessToken() == null || config.getWabaId() == null) {
                continue;
            }
            try {
                syncForConfig(config);
            } catch (Exception e) {
                log.error("WhatsApp template sync hatası: tenantId={}, hata={}", config.getTenantId(), e.getMessage());
            }
        }

        log.info("WhatsApp template sync job'ı tamamlandı.");
    }

    private void syncForConfig(WhatsappConfig config) throws Exception {
        String url = GRAPH_API_URL + "/" + config.getWabaId() + "/message_templates";

        String response = webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + config.getAccessToken())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        if (response == null) {
            return;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode data = root.path("data");

        if (!data.isArray()) {
            return;
        }

        for (JsonNode node : data) {
            String name = node.path("name").asText(null);
            String statusStr = node.path("status").asText(null);

            if (name == null || statusStr == null) {
                continue;
            }

            WhatsappTemplate.TemplateDurum durum = mapStatus(statusStr);
            if (durum == null) {
                continue;
            }

            templateRepository.findByTenantIdAndTemplateKey(config.getTenantId(), name)
                    .ifPresent(template -> {
                        template.setDurum(durum);
                        templateRepository.save(template);
                        log.debug("Template güncellendi: tenantId={}, key={}, durum={}", config.getTenantId(), name, durum);
                    });
        }
    }

    private WhatsappTemplate.TemplateDurum mapStatus(String metaStatus) {
        return switch (metaStatus.toUpperCase()) {
            case "APPROVED" -> WhatsappTemplate.TemplateDurum.APPROVED;
            case "REJECTED" -> WhatsappTemplate.TemplateDurum.REJECTED;
            case "PENDING" -> WhatsappTemplate.TemplateDurum.PENDING;
            case "PAUSED" -> WhatsappTemplate.TemplateDurum.PAUSED;
            case "DISABLED" -> WhatsappTemplate.TemplateDurum.DISABLED;
            default -> null;
        };
    }
}
