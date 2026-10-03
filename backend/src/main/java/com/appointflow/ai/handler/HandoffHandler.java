package com.appointflow.ai.handler;

import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.intent.IntentResult;
import com.appointflow.ai.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandoffHandler implements FlowHandler {

    private final AiConfigService aiConfigService;

    @Override
    public String handle(String conversationId, String userMessage, IntentResult intentResult, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        log.warn("Handoff tetiklendi — conversation: {}, intent: {}, mesaj: {}",
                conversationId, intentResult.getIntent(), userMessage.substring(0, Math.min(50, userMessage.length())));

        // TODO FAZ 4.7: Gercek handoff mekanizmasi (conversation durumu degistirme, bildirim gonderme)
        // Su an sadece mesaj donuyoruz

        return "Talebinizi anlıyorum. Sizi hemen bir yetkiliye yönlendiriyorum. " +
                "Kısa süre içinde size dönüş yapılacaktır. Lütfen bekleyin.";
    }
}
