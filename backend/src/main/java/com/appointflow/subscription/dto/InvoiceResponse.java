package com.appointflow.subscription.dto;

import com.appointflow.subscription.entity.Invoice;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceResponse {
    private Long id;
    private BigDecimal tutar;
    private String paraBirimi;
    private String durum;
    private String donemBaslangic;
    private String donemBitis;
    private String odemeTarihi;

    public static InvoiceResponse from(Invoice inv) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .tutar(inv.getTutar())
                .paraBirimi(inv.getParaBirimi())
                .durum(inv.getDurum())
                .donemBaslangic(inv.getDonemBaslangic() != null ? inv.getDonemBaslangic().toString() : null)
                .donemBitis(inv.getDonemBitis() != null ? inv.getDonemBitis().toString() : null)
                .odemeTarihi(inv.getOdemeTarihi() != null ? inv.getOdemeTarihi().toString() : null)
                .build();
    }
}
