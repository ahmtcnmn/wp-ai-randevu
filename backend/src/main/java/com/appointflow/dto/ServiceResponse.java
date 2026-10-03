package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ServiceResponse {
    private Long id;
    private String ad;
    private String aciklama;
    private Integer sureDakika;
    private Double fiyat;
    private Long kategoriId;
    private String kategoriAd;
    private Integer bufferOnceDk;
    private Integer bufferSonraDk;
    private String takvimRengi;
    private Boolean aktif;
    private List<Long> staffIds;
}
