package com.appointflow.ai.handler;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationContext {
    private final Long tenantId;
    private final Long conversationId;
    private final Long customerId;
    private final String customerPhone;
    private final String customerFullName;
}
