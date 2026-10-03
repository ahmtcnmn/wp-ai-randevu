package com.appointflow.ai.handler;

import com.appointflow.ai.client.OpenAiApiClient;
import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.intent.IntentResult;
import com.appointflow.ai.service.AiConfigService;
import com.appointflow.conversation.repository.ConversationRepository;
import com.appointflow.entity.Randevu;
import com.appointflow.entity.RandevuDurumu;
import com.appointflow.repository.RandevuRepository;
import com.appointflow.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationFlowHandler implements FlowHandler {

    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final java.util.regex.Pattern FAKE_CONFIRM_PATTERN = java.util.regex.Pattern.compile(
            "(iptal ?et(ti|tim|tik)|iptal ?edild|kaldirildi|kaldırıldı|silindi|basariyla|başarıyla)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private final OpenAiApiClient openAiApiClient;
    private final AiConfigService aiConfigService;
    private final AppointmentService appointmentService;
    private final RandevuRepository randevuRepository;
    private final ObjectMapper objectMapper;
    private final ConversationHistoryLoader historyLoader;
    private final ConversationRepository conversationRepository;

    @Override
    public String handle(String conversationId, String userMessage, IntentResult intentResult, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        List<Randevu> activeAppointments = findActiveAppointments(ctx.getTenantId(), ctx.getCustomerId());

        if (activeAppointments.isEmpty()) {
            return "Üzgünüm, kayıtlı aktif bir randevunuz bulunmuyor. Yardımcı olabileceğim başka bir şey var mı?";
        }

        String randevuListesi = activeAppointments.stream()
                .map(r -> String.format("- id=%d, tarih=%s, uzman=%s %s, hizmet=%s, durum=%s",
                        r.getId(),
                        r.getTarihSaat().format(DT),
                        r.getUzman().getAd(),
                        r.getUzman().getSoyad(),
                        r.getHizmet() != null ? r.getHizmet().getAd() : "-",
                        r.getDurum()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format("""
                %s

                Müşteri randevu iptal etmek istiyor.

                MÜŞTERİ:
                - Adı: %s
                - Telefon: %s

                MÜŞTERİNİN AKTİF RANDEVULARI:
                %s

                KURALLAR:
                - Müşteriye randevu listesini göster, hangisini iptal etmek istediğini öğren.
                - Sadece bir aktif randevu varsa, hangisini iptal edeceğini sormaya gerek yok ama yine de teyit al.
                - Tarih, saat veya uzman adı eşleştirmesi ile randevuyu belirle.
                - Teyit aldıktan sonra "cancel_appointment" toolunu çağır. Toolu çağırmadan iptali tamamlamış gibi davranma.
                - Türkçe, kısa ve nazik yanıt ver.
                """,
                config.getSistemPromptu(),
                ctx.getCustomerFullName() != null ? ctx.getCustomerFullName() : "Müşteri",
                ctx.getCustomerPhone(),
                randevuListesi
        );

        List<OpenAiChatRequest.Tool> tools = buildTools();

        // Mevcut session'i (30dk bosluksuz blok) yukle
        List<OpenAiChatRequest.Message> messages = new ArrayList<>(
                historyLoader.loadCurrentSession(ctx.getConversationId(), 1));
        messages.add(OpenAiChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        boolean fakeDetected = false;
        boolean cancelActuallyDone = false;
        for (int iter = 0; iter < MAX_TOOL_ITERATIONS; iter++) {
            Object toolChoice = fakeDetected ? "required" : null;
            String activePrompt = fakeDetected
                    ? systemPrompt + "\n\nDIKKAT: Onceki yanitin tool cagirmadan iptal etmis gibi davraniyordu. Bu YASAK. SIMDI 'cancel_appointment' tool'unu MUTLAKA cagir."
                    : systemPrompt;

            // Iptal akisinda gpt-4o kullan — tool discipline icin gerekli
            OpenAiChatResponse response = openAiApiClient.sendMessage(
                    activePrompt, messages, "gpt-4o", config.getMaxToken(), tools, toolChoice);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return "Üzgünüm, şu anda yanıt veremiyorum.";
            }

            if (!response.hasToolCalls()) {
                String text = response.getTextContent();
                if (!cancelActuallyDone && !fakeDetected && text != null
                        && FAKE_CONFIRM_PATTERN.matcher(text).find()) {
                    log.warn("AI fake cancel onayi tespit edildi: {}", text.substring(0, Math.min(100, text.length())));
                    fakeDetected = true;
                    continue;
                }
                return text;
            }

            List<OpenAiChatResponse.ToolCall> toolCalls = response.getToolCalls();

            List<OpenAiChatRequest.ToolCall> requestToolCalls = toolCalls.stream()
                    .map(tc -> OpenAiChatRequest.ToolCall.builder()
                            .id(tc.getId())
                            .type("function")
                            .function(OpenAiChatRequest.FunctionCall.builder()
                                    .name(tc.getFunction() != null ? tc.getFunction().getName() : null)
                                    .arguments(tc.getFunction() != null ? tc.getFunction().getArguments() : null)
                                    .build())
                            .build())
                    .toList();
            messages.add(OpenAiChatRequest.Message.builder()
                    .role("assistant")
                    .content(null)
                    .toolCalls(requestToolCalls)
                    .build());

            for (OpenAiChatResponse.ToolCall tc : toolCalls) {
                String toolResult = executeTool(tc.getName(), tc.getInputAsMap(), ctx);
                if ("cancel_appointment".equals(tc.getName()) && toolResult.contains("\"ok\":true")) {
                    cancelActuallyDone = true;
                }
                messages.add(OpenAiChatRequest.Message.builder()
                        .role("tool")
                        .toolCallId(tc.getId())
                        .content(toolResult)
                        .build());
            }
        }

        return "İptal işlemi tamamlanamadı, lütfen tekrar deneyin.";
    }

    private List<Randevu> findActiveAppointments(Long tenantId, Long customerId) {
        return randevuRepository.findByTenantIdAndCustomerId(tenantId, customerId).stream()
                .filter(r -> r.getDurum() != RandevuDurumu.IPTAL_EDILDI
                        && r.getDurum() != RandevuDurumu.TAMAMLANDI
                        && r.getDurum() != RandevuDurumu.GELMEDI)
                .filter(r -> r.getTarihSaat().isAfter(LocalDateTime.now().minusHours(1)))
                .toList();
    }

    private String executeTool(String name, Map<String, Object> input, ConversationContext ctx) {
        try {
            if ("cancel_appointment".equals(name)) {
                Long randevuId = asLong(input.get("randevu_id"));
                String neden = asString(input.get("neden"));
                if (randevuId == null) return jsonError("Eksik parametre: randevu_id.");

                Randevu r = randevuRepository.findById(randevuId).orElse(null);
                if (r == null || !r.getTenantId().equals(ctx.getTenantId())
                        || r.getCustomer() == null
                        || !r.getCustomer().getId().equals(ctx.getCustomerId())) {
                    return jsonError("Bu randevu size ait degil veya bulunamadi.");
                }

                appointmentService.cancel(randevuId, neden != null ? neden : "Musteri WhatsApp uzerinden iptal etti");

                // Iptal yapildi — aktif handler'i temizle
                if (ctx.getConversationId() != null) {
                    conversationRepository.findById(ctx.getConversationId()).ifPresent(c -> {
                        c.setAktifHandler(null);
                        conversationRepository.save(c);
                    });
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("ok", true);
                result.put("randevu_id", randevuId);
                result.put("durum", "IPTAL_EDILDI");
                return toJson(result);
            }
            return jsonError("Bilinmeyen arac: " + name);
        } catch (Exception e) {
            log.warn("Iptal tool hatasi — {}", e.getMessage());
            return jsonError(e.getMessage());
        }
    }

    private List<OpenAiChatRequest.Tool> buildTools() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "randevu_id", Map.of("type", "integer", "description", "Iptal edilecek randevunun id'si"),
                        "neden", Map.of("type", "string", "description", "Iptal nedeni (opsiyonel)")
                ),
                "required", List.of("randevu_id")
        );
        return List.of(
                OpenAiChatRequest.Tool.builder()
                        .type("function")
                        .function(OpenAiChatRequest.FunctionDef.builder()
                                .name("cancel_appointment")
                                .description("Belirtilen randevu id'sini iptal eder.")
                                .parameters(schema)
                                .build())
                        .build()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String jsonError(String msg) {
        return toJson(Map.of("ok", false, "error", msg != null ? msg : "unknown"));
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String asString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
