package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ReminderTemplateResponse {
    private Long id;
    private String ad;
    private String mesaj;
    private Integer gunSonra;
    private Boolean oncesi;
    private String birim;
    private Long hizmetId;
    private String kanal;
    private Boolean aktif;
}
