package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FeatureResponse {
    private String featureKey;
    private String ad;
    private String aciklama;
    private Boolean enabled;
}
