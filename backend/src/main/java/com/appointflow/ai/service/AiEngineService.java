package com.appointflow.ai.service;

import com.appointflow.ai.client.OpenAiApiClient;
import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.handler.ConversationContext;
import com.appointflow.ai.handler.ConversationHistoryLoader;
import com.appointflow.ai.tool.PanelAssistantTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEngineService {

    private final OpenAiApiClient openAiApiClient;
    private final AiConfigService aiConfigService;
    private final ConversationHistoryLoader historyLoader;
    @Autowired(required = false)
    private PanelAssistantTools panelTools;

    /** Tool calling loop'ta güvenlik için maksimum tool çalıştırma sayısı. */
    private static final int MAX_TOOL_ITERATIONS = 5;

    /**
     * Basit dil tespiti — Türkçe diakritikleri veya yaygın TR kelimeleri varsa "tr".
     * Yoksa "en" döner (default fallback). Gelişmiş tespit için lib gerekmiyor.
     */
    private String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "tr";
        String lower = text.toLowerCase();
        if (lower.matches(".*[şçğıöü].*")) return "tr";
        if (lower.matches(".*\\b(merhaba|selam|randevu|teşekkür|nasıl|fiyat|nerede|saat|gün|hizmet|iyi)\\b.*")) return "tr";
        return "en";
    }

    /**
     * WhatsApp / müşteri akışı (ConversationFlowService tarafından çağrılır).
     * Müşteriyle WhatsApp üzerinden konuşan asistan — tool erişimi yok, guardrail aktif.
     */
    public String chat(String userMessage, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        if (!config.getAktif()) {
            log.warn("AI devre disi — tenant: {}", config.getTenantId());
            return "AI asistan şu anda devre dışı. Lütfen işletmeyle doğrudan iletişime geçin.";
        }

        List<OpenAiChatRequest.Message> messages = new ArrayList<>(
                historyLoader.loadCurrentSession(ctx != null ? ctx.getConversationId() : null, 1));

        messages.add(OpenAiChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        // Basit dil tespiti — Türkçe karakterler veya yaygın TR kelimeler varsa TR, yoksa EN
        String detectedLang = detectLanguage(userMessage);
        String langInstruction = detectedLang.equals("tr")
                ? "Yanıtını Türkçe ver."
                : "Reply in " + detectedLang + ".";

        String hardenedPrompt = config.getSistemPromptu() + "\n\n" + langInstruction + """


                ÇOK ÖNEMLİ GÜVENLİK KURALI:
                - Sen ŞU AN bir araç (tool) çağıramazsın. DB'ye erişimin yok.
                - **ASLA** "randevunuzu oluşturdum", "iptal ettim", "kaydettim" gibi cümleler KURMA.
                - Müşteri randevu almak istiyorsa SADECE şu cümleyle yönlendir: "Randevu için size yardımcı olabilirim, lütfen istediğiniz hizmeti söyleyin."
                - Müşteri iptal istiyorsa: "İptal işlemi için size yardımcı olabilirim, lütfen iptal etmek istediğiniz randevuyu belirtin."
                - Müsait saat sorulursa: "Müsait saatleri kontrol edebilmem için hizmet ve gün bilgisi gerekli."
                - HİÇBİR koşulda saat veya tarih UYDURUR gibi cevap verme.
                """;

        OpenAiChatResponse response = openAiApiClient.sendMessage(
                hardenedPrompt,
                messages,
                config.getModel(),
                config.getMaxToken()
        );

        String assistantReply = response.getTextContent();

        log.info("AI (WhatsApp) yanit uretildi — conversationId: {}, input: {} tokens, output: {} tokens",
                ctx != null ? ctx.getConversationId() : null,
                response.getUsage() != null ? response.getUsage().getInputTokens() : 0,
                response.getUsage() != null ? response.getUsage().getOutputTokens() : 0);

        return assistantReply;
    }

    /**
     * Dashboard (giriş yapmış kullanıcı) için panel asistanı.
     * Kullanıcı işletme sahibi/admin/staff — müşteri DEĞİL.
     * Bu yüzden WhatsApp guardrail prompt'unu kullanma; panel kullanım rehberi modunda yanıtla.
     */
    public String chatPanelAssistant(String userMessage, String userRole, String userName) {
        AiConfig config = aiConfigService.getConfigEntity();

        if (!config.getAktif()) {
            log.warn("AI devre disi — tenant: {}", config.getTenantId());
            return "AI asistan şu anda devre dışı.";
        }

        String isletmeAciklamasi = config.getIsletmeAciklamasi();
        String isletmeContextLine = (isletmeAciklamasi != null && !isletmeAciklamasi.isBlank())
                ? "\n\nİşletme hakkında: " + isletmeAciklamasi.trim()
                : "";

        String systemPrompt = """
                Sen AppointFlow yönetim panelinde çalışan bir asistansın.
                Karşındaki kişi işletmenin sahibi veya çalışanıdır (müşteri DEĞİL).

                Görevin: AppointFlow uygulamasının nasıl kullanılacağı, hangi özelliklerin nerede olduğu,
                iş süreçleri ve kısa yol önerileri konusunda yardım etmektir.

                Karşındaki kişinin bilgileri:
                - Adı: %s
                - Rolü: %s

                Cevap kuralları:
                - KISA ve NET cevap ver (3-5 cümleyi geçme).
                - Türkçe, profesyonel ama samimi bir dil kullan.
                - Bilmediğin veya doğrulayamadığın bir veriyi UYDURMA. "Bu bilgiyi şu sayfadan kontrol edebilirsiniz" diye yönlendir.
                - Sayısal veri sorulursa (kaç randevum var, bugünkü ciro vb.) önce uygun TOOL'u çağır. Tool sonucu varsa onu kullan.
                - Müşteri yazışması gibi davranma; sen müşterilere değil, işletme sahibine yardım ediyorsun.

                AppointFlow panel navigasyon rehberi:
                - Randevular: /takvim (haftalık), /randevular (liste), /randevular/yeni (yeni randevu)
                - Müşteriler: /musteriler (liste + segmentler), /musteriler/yeni, /musteriler/import (CSV)
                - Hizmetler: /hizmetler (liste + kategoriler)
                - Ürünler: /urunler, /urunler/raporlar (satış raporu)
                - Çalışanlar: /calisanlar, /calisanlar/komisyon, /calisanlar/kazanc, /calisanlar/feedback (şikayetler)
                - Kampanya: /kampanyalar (slot + segment kampanyaları)
                - Rapor: /raporlar (randevu, ciro, müşteri, kampanya sekmeleri)
                - Finans: /finans (abonelik), /finans/faturalar
                - Hatırlatma: /hatirlatma (gönderim geçmişi), /hatirlatma/sablonlar
                - WhatsApp: /whatsapp (canlı konuşmalar), /ayarlar/whatsapp (Meta config)
                - AI: /ayarlar/ai (asistan persona + prompt + handoff)
                - Ayarlar: /ayarlar/subeler, /ayarlar/ozellikler (feature toggle), /ayarlar/iptal-politikasi
                - Hesap: /hesap, /hesap/sifre, /hesap/2fa, /hesap/oturumlar
                - Audit log: /audit-log (OWNER/ADMIN)
                - Süper Admin (sadece SUPER_ADMIN): /sistem/tenants, /sistem/saglik, /sistem/jobs, /sistem/contact-requests
                """.formatted(userName != null ? userName : "kullanıcı", userRole != null ? userRole : "STAFF")
                + isletmeContextLine;

        List<OpenAiChatRequest.Message> messages = new ArrayList<>();
        messages.add(OpenAiChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        List<OpenAiChatRequest.Tool> tools = panelTools != null ? panelTools.definitions() : null;
        int inputTokens = 0, outputTokens = 0;
        String assistantReply = "";

        // Tool calling loop — model tool çağırırsa execute edip cevapla, tekrar dön
        for (int iter = 0; iter < MAX_TOOL_ITERATIONS; iter++) {
            OpenAiChatResponse response = openAiApiClient.sendMessage(
                    systemPrompt,
                    messages,
                    config.getModel(),
                    Math.min(config.getMaxToken(), 600),
                    tools);

            if (response.getUsage() != null) {
                inputTokens += response.getUsage().getInputTokens();
                outputTokens += response.getUsage().getOutputTokens();
            }

            if (!response.hasToolCalls() || tools == null) {
                assistantReply = response.getTextContent();
                break;
            }

            // Modelin tool çağrılarını mesaj listesine ekle
            List<OpenAiChatRequest.ToolCall> requestToolCalls = response.getToolCalls().stream()
                    .map(tc -> OpenAiChatRequest.ToolCall.builder()
                            .id(tc.getId())
                            .type("function")
                            .function(OpenAiChatRequest.FunctionCall.builder()
                                    .name(tc.getName())
                                    .arguments(tc.getFunction() != null ? tc.getFunction().getArguments() : "{}")
                                    .build())
                            .build())
                    .toList();

            messages.add(OpenAiChatRequest.Message.builder()
                    .role("assistant")
                    .toolCalls(requestToolCalls)
                    .build());

            // Her tool'u çalıştır, sonucunu tool mesajı olarak ekle
            for (OpenAiChatResponse.ToolCall tc : response.getToolCalls()) {
                String result = panelTools.execute(tc.getName(), tc.getInputAsMap());
                log.debug("AI tool çağrısı: {} → {}", tc.getName(), result);
                messages.add(OpenAiChatRequest.Message.builder()
                        .role("tool")
                        .toolCallId(tc.getId())
                        .content(result)
                        .build());
            }
        }

        log.info("AI (panel) yanit uretildi — user: {}, role: {}, input: {} tokens, output: {} tokens",
                userName, userRole, inputTokens, outputTokens);

        return assistantReply;
    }
}
