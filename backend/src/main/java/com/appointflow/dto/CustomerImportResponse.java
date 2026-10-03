package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CustomerImportResponse {
    private int toplam;
    private int basarili;
    private List<ImportError> hatalar;

    @Data
    @AllArgsConstructor
    public static class ImportError {
        private int satir;
        private String mesaj;
    }
}
