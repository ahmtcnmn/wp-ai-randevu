package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SegmentCampaignRequest {

    @NotBlank
    private String baslik;

    @NotBlank
    private String hedefSegment; // SegmentType enum değeri

    @NotBlank
    private String mesaj;
}
