package com.appointflow.whatsapp.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class TimestampValidator {

    @Value("${app.whatsapp.timestamp-tolerance-seconds:300}")
    private int toleranceSeconds;

    /**
     * Timestamp'in tolerans suresi icinde olup olmadigini kontrol eder (replay koruması).
     */
    public boolean isValid(long timestamp) {
        long now = Instant.now().getEpochSecond();
        long diff = Math.abs(now - timestamp);

        if (diff > toleranceSeconds) {
            log.warn("Timestamp tolerans disi — fark: {}s, tolerans: {}s", diff, toleranceSeconds);
            return false;
        }

        return true;
    }
}
