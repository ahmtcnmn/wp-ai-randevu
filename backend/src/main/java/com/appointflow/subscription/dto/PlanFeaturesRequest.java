package com.appointflow.subscription.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlanFeaturesRequest {
    private List<String> featureKeys;
    private boolean aktif;
}
