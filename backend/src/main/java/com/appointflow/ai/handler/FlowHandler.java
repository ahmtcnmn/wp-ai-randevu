package com.appointflow.ai.handler;

import com.appointflow.ai.intent.IntentResult;

public interface FlowHandler {
    String handle(String conversationId, String userMessage, IntentResult intentResult, ConversationContext context);
}
