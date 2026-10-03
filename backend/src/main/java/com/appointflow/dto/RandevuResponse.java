package com.appointflow.dto;

import com.appointflow.entity.RandevuDurumu;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RandevuResponse {
    private Long id;
    private String musteriAd;
    private String uzmanAd;
    private String hizmetAd;
    private Double fiyat;
    private Integer sureDakika;
    private LocalDateTime tarihSaat;
    private RandevuDurumu durum;
    private String not;
}
