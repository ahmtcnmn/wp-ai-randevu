package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ServiceCategoryResponse {
    private Long id;
    private String ad;
    private String aciklama;
    private Integer sira;
    private String takvimRengi;
    private Boolean aktif;
}
