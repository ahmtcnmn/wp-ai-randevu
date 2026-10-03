package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductSalesSummaryResponse {
    private BigDecimal toplamCiro;
    private long toplamAdet;
    private long toplamIslem;
    private List<ProductSummary> urunBazli;
    private List<StaffSummary> calisanBazli;

    @Data
    @Builder
    public static class ProductSummary {
        private Long productId;
        private String productAd;
        private long adet;
        private BigDecimal ciro;
    }

    @Data
    @Builder
    public static class StaffSummary {
        private Long staffId;
        private String staffAd;
        private long islem;
        private BigDecimal ciro;
        private BigDecimal toplamKomisyon;
    }
}
