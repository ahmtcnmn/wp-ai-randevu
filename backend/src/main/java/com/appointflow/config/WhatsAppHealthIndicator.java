package com.appointflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component("whatsapp")
public class WhatsAppHealthIndicator implements HealthIndicator {

    private final WebClient webClient = WebClient.create();

    @Value("${app.whatsapp.token:}")
    private String whatsappToken;

    @Override
    public Health health() {
        if (whatsappToken == null || whatsappToken.isBlank()) {
            return Health.unknown().withDetail("reason", "WhatsApp not configured").build();
        }

        try {
            webClient.get()
                    .uri("https://graph.facebook.com")
                    .retrieve()
                    .toBodilessEntity()
                    .block(java.time.Duration.ofSeconds(3));
            return Health.up().withDetail("endpoint", "graph.facebook.com reachable").build();
        } catch (Exception ex) {
            return Health.down().withDetail("error", ex.getMessage()).build();
        }
    }
}
