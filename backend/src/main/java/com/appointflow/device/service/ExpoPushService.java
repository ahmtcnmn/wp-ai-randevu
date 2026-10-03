package com.appointflow.device.service;

import com.appointflow.device.entity.UserDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final UserDeviceService userDeviceService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.push.enabled:false}")
    private boolean enabled;

    public void pushToUser(Long userId, String title, String body, String link) {
        if (!enabled) {
            log.info("[PUSH-LOG] userId={} title={} body={} link={}", userId, title, body, link);
            return;
        }
        List<UserDevice> devices = userDeviceService.findTokensForUser(userId);
        for (UserDevice d : devices) {
            try {
                sendOne(d.getExpoPushToken(), title, body, link);
            } catch (Exception e) {
                log.warn("Push gonderilemedi: userId={}, token={}, err={}", userId, mask(d.getExpoPushToken()), e.getMessage());
            }
        }
    }

    private void sendOne(String to, String title, String body, String link) {
        Map<String, Object> payload = Map.of(
                "to", to,
                "title", title,
                "body", body,
                "sound", "default",
                "data", Map.of("link", link == null ? "" : link)
        );
        webClientBuilder.build()
                .post()
                .uri(EXPO_PUSH_URL)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> log.debug("Expo push response: {}", resp))
                .block();
    }

    private String mask(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
