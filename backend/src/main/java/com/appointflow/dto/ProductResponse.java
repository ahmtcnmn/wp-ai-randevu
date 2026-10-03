package com.appointflow.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ProductResponse {

    private Long id;
    private String ad;
    private String aciklama;
    private BigDecimal fiyat;
    private Integer stok;
    private String kategori;
    private Boolean aiOneriAktif;
    private Boolean aktif;
    private List<Long> hizmetIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
