package com.appointflow.ai.intent;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentResult {
    private Intent intent;
    private double confidence;
    private Map<String, String> extractedEntities;
}
