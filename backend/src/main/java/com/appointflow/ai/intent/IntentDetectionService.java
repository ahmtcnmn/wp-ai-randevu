package com.appointflow.ai.intent;

import com.appointflow.ai.client.OpenAiApiClient;
import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentDetectionService {

    private final OpenAiApiClient openAiApiClient;
    private final ObjectMapper objectMapper;

    private static final String INTENT_SYSTEM_PROMPT = """
            Sen bir intent detection (niyet tespiti) sistemisin. Kullanicinin mesajini analiz edip JSON formatinda yanit ver.

            Mumkun intent'ler:
            - BOOK_APPOINTMENT: Randevu almak istiyor
            - CANCEL_APPOINTMENT: Mevcut randevuyu iptal etmek istiyor
            - QUERY_APPOINTMENT: Randevusu hakkinda bilgi istiyor (ne zaman, var mi, vb.)
            - RESCHEDULE: Randevuyu baska zamana tasimak istiyor
            - SERVICE_INFO: Hizmetler, fiyatlar, sureler hakkinda bilgi istiyor
            - PRODUCT_INQUIRY: Urunler hakkinda soru soruyor
            - COMPLAINT: Sikayet ediyor, memnuniyetsiz
            - STAFF_REQUEST: Belirli bir calisan/uzman ile gorusmek istiyor
            - GENERAL_CHAT: Yukaridakilerin hicbirine uymuyor, genel sohbet

            Ayrica mesajdan cikarabildigin bilgileri de ekle:
            - tarih: Bahsedilen tarih (varsa)
            - saat: Bahsedilen saat (varsa)
            - hizmet: Bahsedilen hizmet (varsa)
            - calisan: Bahsedilen calisan adi (varsa)

            SADECE asagidaki JSON formatinda yanit ver, baska hicbir sey yazma:
            {"intent":"INTENT_ADI","confidence":0.95,"entities":{"tarih":"","saat":"","hizmet":"","calisan":""}}
            """;

    public IntentResult detectIntent(String userMessage) {
        try {
            List<OpenAiChatRequest.Message> messages = List.of(
                    OpenAiChatRequest.Message.builder()
                            .role("user")
                            .content(userMessage)
                            .build()
            );

            OpenAiChatResponse response = openAiApiClient.sendMessage(
                    INTENT_SYSTEM_PROMPT, messages, "gpt-4o-mini", 256);

            String text = response.getTextContent().trim();
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                text = text.substring(jsonStart, jsonEnd + 1);
            }

            Map<String, Object> parsed = objectMapper.readValue(text, new TypeReference<>() {});

            String intentStr = (String) parsed.getOrDefault("intent", "GENERAL_CHAT");
            double confidence = parsed.containsKey("confidence")
                    ? ((Number) parsed.get("confidence")).doubleValue()
                    : 0.5;

            @SuppressWarnings("unchecked")
            Map<String, String> entities = parsed.containsKey("entities")
                    ? objectMapper.convertValue(parsed.get("entities"), new TypeReference<>() {})
                    : new HashMap<>();

            entities.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isBlank());

            Intent intent;
            try {
                intent = Intent.valueOf(intentStr);
            } catch (IllegalArgumentException e) {
                intent = Intent.GENERAL_CHAT;
                confidence = 0.3;
            }

            log.info("Intent tespit edildi: {} (confidence: {}) — mesaj: {}",
                    intent, confidence, userMessage.substring(0, Math.min(50, userMessage.length())));

            return IntentResult.builder()
                    .intent(intent)
                    .confidence(confidence)
                    .extractedEntities(entities)
                    .build();

        } catch (Exception e) {
            log.error("Intent tespit hatasi: {}", e.getMessage());
            return IntentResult.builder()
                    .intent(Intent.GENERAL_CHAT)
                    .confidence(0.0)
                    .extractedEntities(new HashMap<>())
                    .build();
        }
    }
}
