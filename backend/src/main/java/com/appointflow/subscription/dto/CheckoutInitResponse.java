package com.appointflow.subscription.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckoutInitResponse {
    private String checkoutFormContent;
    private String token;
    private String planKey;
}
