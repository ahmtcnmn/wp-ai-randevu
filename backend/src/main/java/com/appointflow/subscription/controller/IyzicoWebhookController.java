package com.appointflow.subscription.controller;

import com.appointflow.subscription.dto.IyzicoWebhookEvent;
import com.appointflow.subscription.service.IyzicoWebhookProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook/iyzico")
@RequiredArgsConstructor
public class IyzicoWebhookController {

    private final IyzicoWebhookProcessor processor;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-iyz-signature", required = false) String signature) {

        if (!processor.verifySignature(payload, signature)) {
            log.warn("İyzico webhook imza doğrulaması başarısız.");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            IyzicoWebhookEvent event = objectMapper.readValue(payload, IyzicoWebhookEvent.class);
            processor.process(event);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("İyzico webhook işleme hatası: {}", e.getMessage());
            return ResponseEntity.status(500).body("Processing error");
        }
    }
}
