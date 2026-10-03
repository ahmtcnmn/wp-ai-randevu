package com.appointflow.ai.handler;

import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.conversation.entity.Message;
import com.appointflow.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationHistoryLoader {

    /** Iki mesaj arasi bu kadar dakika gectiyse yeni "session" baslamis kabul edilir. */
    private static final long SESSION_GAP_MINUTES = 30;

    private static final Pattern FAKE_AI_CONFIRM = Pattern.compile(
            "(kaydet|olusturdum|oluşturdum|olusturuyorum|oluşturuyorum|kaydediyorum|" +
                    "ayarlandi|ayarlandı|onaylandi|onaylandı|basariyla|başarıyla|" +
                    "randevu(n|nu)?z .*alindi|randevu(n|nu)?z .*alındı|" +
                    "iptal ?et(ti|tim|tik)|iptal ?edild|kaldirildi|kaldırıldı|silindi)",
            Pattern.CASE_INSENSITIVE);

    private final MessageRepository messageRepository;

    /**
     * DB'deki messages tablosundan konusma gecmisini OpenAI Message formatina cevirir.
     * Son mesaj (yeni gelen) DAHIL DEGIL — caller user message'i ayrica ekler.
     * Tool result/tool_call mesajlari atlanir, sadece user/assistant text mesajlari.
     */
    /**
     * Son "session"i (kesintisiz mesaj blogu) yukler.
     * Iki mesaj arasi SESSION_GAP_MINUTES dakikadan fazla gectiyse, oncesi farkli session sayilir.
     * AI ve CUSTOMER mesajlari dahil — fake confirmler filtrelenir.
     * Kullanim: BookingFlowHandler ve CancellationFlowHandler.
     */
    public List<OpenAiChatRequest.Message> loadCurrentSession(Long conversationId, int excludeLastN) {
        if (conversationId == null) return new ArrayList<>();
        List<Message> all = messageRepository.findByConversationIdOrderByOlusturmaTarihiAsc(conversationId);
        if (all.isEmpty()) return new ArrayList<>();

        int upTo = Math.max(0, all.size() - excludeLastN);
        List<Message> historical = all.subList(0, upTo);
        if (historical.isEmpty()) return new ArrayList<>();

        // Session baslangic indeksini bul: sondan geriye git, gap > SESSION_GAP varsa kes
        int sessionStart = 0;
        for (int i = historical.size() - 1; i > 0; i--) {
            LocalDateTime curr = historical.get(i).getOlusturmaTarihi();
            LocalDateTime prev = historical.get(i - 1).getOlusturmaTarihi();
            if (curr == null || prev == null) continue;
            long gap = Duration.between(prev, curr).toMinutes();
            if (gap >= SESSION_GAP_MINUTES) {
                sessionStart = i;
                break;
            }
        }
        log.debug("Session window: conversationId={}, total={}, sessionStart={}, sessionSize={}",
                conversationId, historical.size(), sessionStart, historical.size() - sessionStart);

        List<OpenAiChatRequest.Message> result = new ArrayList<>();
        for (int i = sessionStart; i < historical.size(); i++) {
            Message m = historical.get(i);
            if (m.getIcerik() == null || m.getIcerik().isBlank()) continue;
            if (m.getMesajTipi() != Message.MesajTipi.TEXT) continue;

            String role = switch (m.getSenderType()) {
                case CUSTOMER -> "user";
                case AI, STAFF -> "assistant";
                case SYSTEM -> null;
            };
            if (role == null) continue;

            // Fake confirm AI mesajlarini at — sonraki AI'i tekrar uydurmaya yonlendirir
            if ("assistant".equals(role) && FAKE_AI_CONFIRM.matcher(m.getIcerik()).find()) {
                continue;
            }

            result.add(OpenAiChatRequest.Message.builder()
                    .role(role)
                    .content(m.getIcerik())
                    .build());
        }
        return result;
    }

    /**
     * Sadece kullanici (user) mesajlarini history'den yukler.
     * Kullanim: BookingFlowHandler ve CancellationFlowHandler — AI'in eski uydurma yanitlari
     * kontaminasyon yaratmasin diye sadece kullanici tarafini al.
     */
    public List<OpenAiChatRequest.Message> loadCustomerHistory(Long conversationId, int excludeLastN) {
        if (conversationId == null) return new ArrayList<>();
        List<Message> all = messageRepository.findByConversationIdOrderByOlusturmaTarihiAsc(conversationId);
        if (all.isEmpty()) return new ArrayList<>();

        int upTo = Math.max(0, all.size() - excludeLastN);
        List<Message> historical = all.subList(0, upTo);

        List<OpenAiChatRequest.Message> result = new ArrayList<>();
        for (Message m : historical) {
            if (m.getIcerik() == null || m.getIcerik().isBlank()) continue;
            if (m.getMesajTipi() != Message.MesajTipi.TEXT) continue;
            if (m.getSenderType() != Message.SenderType.CUSTOMER) continue;
            result.add(OpenAiChatRequest.Message.builder()
                    .role("user")
                    .content(m.getIcerik())
                    .build());
        }
        return result;
    }

    public List<OpenAiChatRequest.Message> loadHistory(Long conversationId, int excludeLastN) {
        if (conversationId == null) return new ArrayList<>();

        List<Message> all = messageRepository.findByConversationIdOrderByOlusturmaTarihiAsc(conversationId);
        if (all.isEmpty()) return new ArrayList<>();

        // Son N mesaji haric tut (caller current user message'i prompt'a ayri ekleyecek)
        int upTo = Math.max(0, all.size() - excludeLastN);
        List<Message> historical = all.subList(0, upTo);

        List<OpenAiChatRequest.Message> result = new ArrayList<>();
        for (Message m : historical) {
            if (m.getIcerik() == null || m.getIcerik().isBlank()) continue;
            if (m.getMesajTipi() != Message.MesajTipi.TEXT) continue;

            String role = switch (m.getSenderType()) {
                case CUSTOMER -> "user";
                case AI, STAFF -> "assistant";
                case SYSTEM -> null;
            };
            if (role == null) continue;

            // AI'in fake "kaydediyorum/iptal ettim" gibi mesajlarini history'den hariç tut.
            // Bu mesajlar gercek bir aksiyon olmadan uretildi, AI'i tekrar uydurmaya yonlendirir.
            if ("assistant".equals(role) && FAKE_AI_CONFIRM.matcher(m.getIcerik()).find()) {
                continue;
            }

            result.add(OpenAiChatRequest.Message.builder()
                    .role(role)
                    .content(m.getIcerik())
                    .build());
        }
        return result;
    }
}
