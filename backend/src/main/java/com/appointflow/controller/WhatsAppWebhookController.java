package com.appointflow.controller;

import com.appointflow.ai.handler.ConversationContext;
import com.appointflow.ai.service.ConversationFlowService;
import com.appointflow.conversation.entity.Conversation;
import com.appointflow.conversation.service.ConversationService;
import com.appointflow.entity.Customer;
import com.appointflow.repository.CustomerRepository;
import com.appointflow.tenant.TenantContext;
import com.appointflow.whatsapp.entity.WhatsappConfig;
import com.appointflow.whatsapp.service.WhatsappConfigService;
import com.appointflow.whatsapp.service.WhatsappMessageService;
import com.appointflow.whatsapp.webhook.MessageDeduplicator;
import com.appointflow.whatsapp.webhook.SignatureVerifier;
import com.appointflow.whatsapp.webhook.TimestampValidator;
import com.appointflow.whatsapp.webhook.WebhookPayloadParser;
import com.appointflow.whatsapp.webhook.WebhookPayloadParser.ParsedMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final SignatureVerifier signatureVerifier;
    private final TimestampValidator timestampValidator;
    private final MessageDeduplicator messageDeduplicator;
    private final WebhookPayloadParser payloadParser;
    private final WhatsappConfigService whatsappConfigService;
    private final WhatsappMessageService whatsappMessageService;
    private final ConversationFlowService conversationFlowService;
    private final ConversationService conversationService;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.whatsapp.webhook-verify-token:}")
    private String webhookVerifyToken;

    /**
     * Meta WhatsApp webhook dogrulama (GET).
     */
    @GetMapping("/whatsapp")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        log.info("WhatsApp webhook dogrulama istegi — mode: {}", mode);

        if (!"subscribe".equals(mode)) {
            return ResponseEntity.status(403).body("Gecersiz mod");
        }

        if (webhookVerifyToken == null || webhookVerifyToken.isBlank()) {
            log.error("WhatsApp webhook verify token konfigure edilmemis (app.whatsapp.webhook-verify-token).");
            return ResponseEntity.status(500).body("Webhook verify token configured edilmemis");
        }

        if (!webhookVerifyToken.equals(token)) {
            log.warn("WhatsApp webhook dogrulama basarisiz — yanlis token");
            return ResponseEntity.status(403).body("Gecersiz token");
        }

        return ResponseEntity.ok(challenge);
    }

    /**
     * Meta WhatsApp webhook mesaj alma (POST).
     * Hemen 200 doner, islemi arka planda yapar.
     */
    @PostMapping("/whatsapp")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        // 1. Payload parse et
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawPayload, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Gecersiz webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("status", "invalid_payload"));
        }

        ParsedMessage message = payloadParser.parse(payload);
        if (message == null) {
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        // 2. Tenant resolution (phone_number_id -> tenant)
        Optional<WhatsappConfig> configOpt = whatsappConfigService.getConfigByPhoneNumberId(message.getPhoneNumberId());
        if (configOpt.isEmpty()) {
            log.warn("Tenant bulunamadi — phoneNumberId: {}", message.getPhoneNumberId());
            return ResponseEntity.ok(Map.of("status", "no_tenant"));
        }
        WhatsappConfig config = configOpt.get();

        // 3. Signature dogrulama
        if (config.getAppSecret() != null && !config.getAppSecret().isBlank()) {
            if (signature == null || !signatureVerifier.verify(config.getAppSecret(), rawPayload, signature)) {
                log.warn("Gecersiz webhook imzasi — phoneNumberId: {}", message.getPhoneNumberId());
                return ResponseEntity.status(401).body(Map.of("status", "invalid_signature"));
            }
        }

        // 4. Timestamp dogrulama (replay korumasi)
        if (!timestampValidator.isValid(message.getTimestamp())) {
            log.warn("Timestamp tolerans disi — messageId: {}", message.getMessageId());
            return ResponseEntity.ok(Map.of("status", "timestamp_expired"));
        }

        // 5. Deduplication
        if (messageDeduplicator.isDuplicate(message.getMessageId())) {
            return ResponseEntity.ok(Map.of("status", "duplicate"));
        }

        // 6. Hemen 200 don, islemi arka planda yap
        CompletableFuture.runAsync(() -> processMessage(message, config));

        return ResponseEntity.ok(Map.of("status", "received"));
    }

    private void processMessage(ParsedMessage message, WhatsappConfig config) {
        try {
            Long tenantId = config.getTenantId();
            TenantContext.set(tenantId);

            // Sadece text mesajlari isle
            if (!"text".equals(message.getType()) || message.getText() == null) {
                log.debug("Text olmayan mesaj atlaniyor — tip: {}", message.getType());
                return;
            }

            // Musteri bul veya olustur
            Customer customer = customerRepository.findByTenantIdAndTelefon(tenantId, message.getFrom())
                    .orElseGet(() -> {
                        Customer newCustomer = Customer.builder()
                                .tenantId(tenantId)
                                .ad(message.getSenderName() != null ? message.getSenderName() : "WhatsApp Müşteri")
                                .soyad("")
                                .telefon(message.getFrom())
                                .build();
                        return customerRepository.save(newCustomer);
                    });

            // Kara listede mi kontrol
            if (customer.getKaraListedeMi()) {
                log.info("Kara listedeki musteriden mesaj — telefon: {}", message.getFrom());
                return;
            }

            // Conversation bul veya olustur
            Conversation conversation = conversationService.findOrCreateConversation(
                    tenantId, customer.getId(), message.getFrom(),
                    customer.getAd() + " " + customer.getSoyad());

            // Konusma HUMAN_ACTIVE ise AI'a yonlendirme
            if (conversation.getDurum() == Conversation.ConversationDurum.HUMAN_ACTIVE) {
                conversationService.saveIncomingMessage(conversation.getId(), message.getText(), message.getMessageId());
                log.info("Insan yonetiyor, AI atlaniyor — conversation: {}", conversation.getId());
                return;
            }

            // Gelen mesaji kaydet
            conversationService.saveIncomingMessage(conversation.getId(), message.getText(), message.getMessageId());

            // AI ile islemi yap
            String conversationKey = "whatsapp:" + message.getFrom();
            ConversationContext ctx = ConversationContext.builder()
                    .tenantId(tenantId)
                    .conversationId(conversation.getId())
                    .customerId(customer.getId())
                    .customerPhone(message.getFrom())
                    .customerFullName((customer.getAd() + " " + customer.getSoyad()).trim())
                    .build();
            String reply = conversationFlowService.processMessage(conversationKey, message.getText(), ctx);

            // AI yanitini kaydet
            conversationService.saveAiResponse(conversation.getId(), reply);

            // Yaniti WhatsApp'tan gonder (test ortaminda token yoksa gonderim hatasi tum akisi bozmasin)
            try {
                whatsappMessageService.sendTextMessage(tenantId, message.getFrom(), reply);
            } catch (Exception sendEx) {
                log.warn("WhatsApp yanit gonderilemedi (tenant: {}): {}", tenantId, sendEx.getMessage());
            }

            log.info("WhatsApp mesaj islendi — from: {}, tenant: {}", message.getFrom(), tenantId);

        } catch (Exception e) {
            log.error("WhatsApp mesaj isleme hatasi: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
