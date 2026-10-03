package com.appointflow.ai.handler;

import com.appointflow.ai.client.OpenAiApiClient;
import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.intent.IntentResult;
import com.appointflow.ai.service.AiConfigService;
import com.appointflow.dto.ServiceResponse;
import com.appointflow.service.HizmetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryFlowHandler implements FlowHandler {

    private final OpenAiApiClient openAiApiClient;
    private final AiConfigService aiConfigService;
    private final HizmetService hizmetService;
    private final ConversationHistoryLoader historyLoader;

    @Override
    public String handle(String conversationId, String userMessage, IntentResult intentResult, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        List<ServiceResponse> hizmetler = hizmetService.getAll();
        String hizmetListesi = hizmetler.stream()
                .map(h -> {
                    String fiyatBilgisi = config.getFiyatBilgisiGoster()
                            ? " - " + h.getFiyat() + " TL" : "";
                    return "- " + h.getAd() + " (" + h.getSureDakika() + " dk)" + fiyatBilgisi;
                })
                .collect(Collectors.joining("\n"));

        String querySystemPrompt = String.format("""
                %s

                Musteri randevu veya hizmet hakkinda bilgi soruyor.

                MEVCUT HIZMETLER:
                %s

                KURALLAR:
                - Musterinin sorusunu anla ve dogru bilgiyi ver
                - Randevu sorgulama icin isim veya telefon bilgisi sor
                - Hizmetler hakkinda detayli bilgi ver
                - Kisa ve net yanitlar ver
                """,
                config.getSistemPromptu(),
                hizmetListesi
        );

        List<OpenAiChatRequest.Message> messages = new java.util.ArrayList<>(
                historyLoader.loadCurrentSession(ctx.getConversationId(), 1));
        messages.add(OpenAiChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        OpenAiChatResponse response = openAiApiClient.sendMessage(
                querySystemPrompt, messages, config.getModel(), config.getMaxToken());

        return response.getTextContent();
    }
}
