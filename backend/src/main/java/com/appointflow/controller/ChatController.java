package com.appointflow.controller;

import com.appointflow.ai.service.AiEngineService;
import com.appointflow.common.ApiResponse;
import com.appointflow.entity.Kullanici;
import com.appointflow.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String DASHBOARD_CHAT_KEY_PREFIX = "chat:dashboard:";

    private final AiEngineService aiEngineService;
    private final StringRedisTemplate redisTemplate;
    private final KullaniciRepository kullaniciRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String mesaj = request.get("mesaj");
        if (mesaj == null || mesaj.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Mesaj boş olamaz"));
        }

        // Dashboard chat: panel asistanı — giriş yapmış kullanıcının rolü ve adı prompt'a iletilir.
        String email = authentication != null ? authentication.getName() : null;
        String userName = "kullanıcı";
        String userRole = "STAFF";
        if (email != null) {
            Kullanici k = kullaniciRepository.findByEmail(email).orElse(null);
            if (k != null) {
                userName = (k.getAd() != null ? k.getAd() : "") +
                        (k.getSoyad() != null ? " " + k.getSoyad() : "");
                userName = userName.isBlank() ? email : userName.trim();
                if (k.getRol() != null) userRole = k.getRol().name();
            }
        }

        String yanit = aiEngineService.chatPanelAssistant(mesaj, userRole, userName);

        return ResponseEntity.ok(ApiResponse.success(Map.of("yanit", yanit)));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email != null) {
            Set<String> keys = redisTemplate.keys(DASHBOARD_CHAT_KEY_PREFIX + email + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
