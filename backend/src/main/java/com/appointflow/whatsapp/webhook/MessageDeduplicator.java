package com.appointflow.whatsapp.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDeduplicator {

    private static final String DEDUP_PREFIX = "whatsapp:dedup:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * Mesajin daha once islenip islenmedigini kontrol eder.
     * @return true ise mesaj cift (daha once islendi), false ise yeni mesaj
     */
    public boolean isDuplicate(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return false;
        }

        String key = DEDUP_PREFIX + messageId;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);

        if (Boolean.FALSE.equals(wasAbsent)) {
            log.debug("Cift mesaj tespit edildi: {}", messageId);
            return true;
        }

        return false;
    }
}
