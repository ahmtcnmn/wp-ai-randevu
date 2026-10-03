package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private String musteriAd;
    private Long customerId;
    private String uzmanAd;
    private Long uzmanId;
    private List<AppointmentServiceItem> hizmetler;
    private LocalDateTime tarihSaat;
    private LocalDateTime bitisTarihi;
    private String durum;
    private String kaynak;
    private Double toplamFiyat;
    /** Bu randevuda satılan ürünlerin toplam tutarı (varsa). */
    private Double urunToplami;
    /** Hizmet + ürünler genel toplamı (toplamFiyat + urunToplami). */
    private Double genelToplam;
    private Double odenenTutar;
    private Integer toplamSureDk;
    private String not;
    private String iptalNedeni;
    private LocalDateTime createdAt;

    @Data
    @AllArgsConstructor
    @Builder
    public static class AppointmentServiceItem {
        private Long hizmetId;
        private String hizmetAd;
        private Double fiyat;
        private Integer sureDk;
    }
}
