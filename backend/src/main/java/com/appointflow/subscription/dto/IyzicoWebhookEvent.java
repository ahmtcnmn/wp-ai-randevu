package com.appointflow.subscription.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IyzicoWebhookEvent {
    private String eventType;
    private String paymentConversationId;
    private String subscriptionReferenceCode;
    private String paymentId;
    private String status;
    private String errorMessage;
}
