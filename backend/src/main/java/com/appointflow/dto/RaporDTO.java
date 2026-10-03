package com.appointflow.dto;

import lombok.Data;

@Data
public class RaporDTO {
    private Double toplamGelir;
    private Long tamamlananRandevuSayisi;
    private Long iptalRandevuSayisi;
    private Long gelmeyenSoruSayisi;
}
