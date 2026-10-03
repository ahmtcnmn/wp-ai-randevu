package com.appointflow.whatsapp.webhook;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebhookPayloadParser {

    /**
     * Meta WhatsApp webhook payload'ından mesaj bilgilerini cikarir.
     */
    @SuppressWarnings("unchecked")
    public ParsedMessage parse(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries == null || entries.isEmpty()) return null;

            Map<String, Object> entry = entries.get(0);
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            if (changes == null || changes.isEmpty()) return null;

            Map<String, Object> change = changes.get(0);
            Map<String, Object> value = (Map<String, Object>) change.get("value");
            if (value == null) return null;

            // Phone number ID (tenant resolution icin)
            Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
            String phoneNumberId = metadata != null ? (String) metadata.get("phone_number_id") : null;

            // Statuses (mesaj durumu bildirimi)
            List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
            if (statuses != null && !statuses.isEmpty()) {
                // Status update — su an loglayip geciyoruz
                log.debug("WhatsApp status update alindi: {}", statuses.get(0).get("status"));
                return null;
            }

            // Messages
            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) return null;

            Map<String, Object> message = messages.get(0);
            String messageId = (String) message.get("id");
            String from = (String) message.get("from");
            String type = (String) message.get("type");
            Object timestampObj = message.get("timestamp");
            long timestamp = timestampObj instanceof String
                    ? Long.parseLong((String) timestampObj)
                    : ((Number) timestampObj).longValue();

            String text = null;
            if ("text".equals(type)) {
                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                text = textObj != null ? (String) textObj.get("body") : null;
            }

            // Gonderici profil bilgisi
            List<Map<String, Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
            String senderName = null;
            if (contacts != null && !contacts.isEmpty()) {
                Map<String, Object> profile = (Map<String, Object>) contacts.get(0).get("profile");
                senderName = profile != null ? (String) profile.get("name") : null;
            }

            return ParsedMessage.builder()
                    .messageId(messageId)
                    .from(from)
                    .senderName(senderName)
                    .type(type)
                    .text(text)
                    .timestamp(timestamp)
                    .phoneNumberId(phoneNumberId)
                    .build();

        } catch (Exception e) {
            log.error("Webhook payload parse hatasi: {}", e.getMessage());
            return null;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParsedMessage {
        private String messageId;
        private String from;
        private String senderName;
        private String type;
        private String text;
        private long timestamp;
        private String phoneNumberId;
    }
}
