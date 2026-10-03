package com.appointflow.ai.service;

import com.appointflow.ai.entity.AiConfig;
import com.appointflow.ai.handler.*;
import com.appointflow.ai.intent.Intent;
import com.appointflow.ai.intent.IntentDetectionService;
import com.appointflow.ai.intent.IntentResult;
import com.appointflow.conversation.entity.Conversation;
import com.appointflow.conversation.entity.Conversation.ActiveHandler;
import com.appointflow.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationFlowService {

    private final IntentDetectionService intentDetectionService;
    private final AiConfigService aiConfigService;
    private final AiEngineService aiEngineService;
    private final BookingFlowHandler bookingFlowHandler;
    private final CancellationFlowHandler cancellationFlowHandler;
    private final QueryFlowHandler queryFlowHandler;
    private final HandoffHandler handoffHandler;
    private final ConversationRepository conversationRepository;

    private static final Set<Intent> HANDOFF_INTENTS = Set.of(Intent.COMPLAINT, Intent.STAFF_REQUEST);
    private static final Set<String> EXIT_KEYWORDS = Set.of(
            "iptal", "vazgec", "vazgeç", "vazgectim", "vazgeçtim",
            "bosver", "boşver", "tamam bosver", "tamam boşver",
            "baska bir sey", "başka bir şey", "yeni konu"
    );
    private static final Set<String> BOOKING_KEYWORDS = Set.of(
            "randevu", "rezerva", "rezervasyon",
            "almak istiyorum", "almak isterim", "almak istiyom",
            "saat var", "saat müsait", "saat musait", "musait saat", "müsait saat",
            "saç kesimi", "sac kesimi", "saç tıraşı", "sac tirasi", "sakal", "tıraş", "tiras",
            "boyama", "kesim", "manikur", "manikür", "pedikür", "pedikur",
            "yarın", "yarin", "bugün", "bugun", "öbür gün", "obur gun",
            "pazartesi", "salı", "sali", "çarşamba", "carsamba", "perşembe", "persembe", "cuma", "cumartesi", "pazar"
    );
    private static final Set<String> CANCEL_KEYWORDS = Set.of(
            "iptal et", "iptal etmek", "iptal istiyorum", "randevumu iptal",
            "vazgec", "vazgeç", "vazgecmek", "vazgeçmek",
            "gelmeyecegim", "gelmeyeceğim"
    );

    public String processMessage(String conversationKey, String userMessage, ConversationContext ctx) {
        AiConfig config = aiConfigService.getConfigEntity();

        if (shouldHandoff(userMessage, config)) {
            clearActiveHandler(ctx);
            IntentResult handoffResult = IntentResult.builder()
                    .intent(Intent.COMPLAINT)
                    .confidence(1.0)
                    .build();
            return handoffHandler.handle(conversationKey, userMessage, handoffResult, ctx);
        }

        // Aktif handler varsa intent'i atla, dogrudan o handler'a git (state machine)
        ActiveHandler active = loadActiveHandler(ctx);
        if (active != null && !isExitKeyword(userMessage)) {
            log.info("Aktif handler'da devam: {} — conversation: {}", active, ctx.getConversationId());
            IntentResult stub = IntentResult.builder()
                    .intent(active == ActiveHandler.BOOKING ? Intent.BOOK_APPOINTMENT : Intent.CANCEL_APPOINTMENT)
                    .confidence(1.0)
                    .build();
            return active == ActiveHandler.BOOKING
                    ? bookingFlowHandler.handle(conversationKey, userMessage, stub, ctx)
                    : cancellationFlowHandler.handle(conversationKey, userMessage, stub, ctx);
        }

        // Cikis kelimesi geldiyse handler'i temizle
        if (active != null && isExitKeyword(userMessage)) {
            clearActiveHandler(ctx);
        }

        IntentResult intentResult = intentDetectionService.detectIntent(userMessage);
        log.info("Conversation {} — intent: {} ({})",
                conversationKey, intentResult.getIntent(), intentResult.getConfidence());

        // Keyword guard: intent OpenAI hatasi veya GENERAL_CHAT/dusuk confidence donduyse,
        // mesajda randevu/iptal kelimesi varsa zorla BOOK/CANCEL'a yonlendir
        boolean intentUnreliable = intentResult.getConfidence() < 0.7
                || intentResult.getIntent() == Intent.GENERAL_CHAT;
        if (intentUnreliable) {
            if (hasCancelKeyword(userMessage)) {
                log.info("Keyword guard CANCEL: {}", userMessage);
                intentResult = IntentResult.builder()
                        .intent(Intent.CANCEL_APPOINTMENT)
                        .confidence(1.0)
                        .build();
            } else if (hasBookingKeyword(userMessage)) {
                log.info("Keyword guard BOOK: {}", userMessage);
                intentResult = IntentResult.builder()
                        .intent(Intent.BOOK_APPOINTMENT)
                        .confidence(1.0)
                        .build();
            }
        }

        if (HANDOFF_INTENTS.contains(intentResult.getIntent())) {
            return handoffHandler.handle(conversationKey, userMessage, intentResult, ctx);
        }

        if (intentResult.getConfidence() < 0.5) {
            return aiEngineService.chat(userMessage, ctx);
        }

        return switch (intentResult.getIntent()) {
            case BOOK_APPOINTMENT, RESCHEDULE -> {
                setActiveHandler(ctx, ActiveHandler.BOOKING);
                yield bookingFlowHandler.handle(conversationKey, userMessage, intentResult, ctx);
            }
            case CANCEL_APPOINTMENT -> {
                setActiveHandler(ctx, ActiveHandler.CANCELLATION);
                yield cancellationFlowHandler.handle(conversationKey, userMessage, intentResult, ctx);
            }
            case QUERY_APPOINTMENT, SERVICE_INFO, PRODUCT_INQUIRY ->
                    queryFlowHandler.handle(conversationKey, userMessage, intentResult, ctx);
            default -> aiEngineService.chat(userMessage, ctx);
        };
    }

    private ActiveHandler loadActiveHandler(ConversationContext ctx) {
        if (ctx == null || ctx.getConversationId() == null) return null;
        return conversationRepository.findById(ctx.getConversationId())
                .map(Conversation::getAktifHandler)
                .orElse(null);
    }

    private void setActiveHandler(ConversationContext ctx, ActiveHandler handler) {
        if (ctx == null || ctx.getConversationId() == null) return;
        conversationRepository.findById(ctx.getConversationId()).ifPresent(c -> {
            c.setAktifHandler(handler);
            conversationRepository.save(c);
        });
    }

    private void clearActiveHandler(ConversationContext ctx) {
        if (ctx == null || ctx.getConversationId() == null) return;
        conversationRepository.findById(ctx.getConversationId()).ifPresent(c -> {
            if (c.getAktifHandler() != null) {
                c.setAktifHandler(null);
                conversationRepository.save(c);
            }
        });
    }

    private boolean isExitKeyword(String message) {
        String norm = message.toLowerCase().trim();
        return EXIT_KEYWORDS.stream().anyMatch(norm::contains);
    }

    private boolean hasBookingKeyword(String message) {
        String norm = message.toLowerCase();
        return BOOKING_KEYWORDS.stream().anyMatch(norm::contains);
    }

    private boolean hasCancelKeyword(String message) {
        String norm = message.toLowerCase();
        return CANCEL_KEYWORDS.stream().anyMatch(norm::contains);
    }

    private boolean shouldHandoff(String message, AiConfig config) {
        if (config.getHandoffKelimeleri() == null || config.getHandoffKelimeleri().isBlank()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return Arrays.stream(config.getHandoffKelimeleri().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(lowerMessage::contains);
    }
}
