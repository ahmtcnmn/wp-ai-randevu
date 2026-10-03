package com.appointflow.service;

import com.appointflow.common.ApiException;
import com.appointflow.dto.CustomerImportResponse;
import com.appointflow.dto.CustomerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV format (UTF-8, comma-separated, ilk satir header):
 *   ad,soyad,telefon,email,notlar
 * Ornek:
 *   Ali,Veli,905551112233,ali@test.com,VIP musteri
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportService {

    private final CustomerService customerService;

    public CustomerImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Dosya bos.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw ApiException.badRequest("Dosya 5MB'dan buyuk olamaz.");
        }

        int toplam = 0;
        int basarili = 0;
        List<CustomerImportResponse.ImportError> hatalar = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw ApiException.badRequest("CSV bos veya gecersiz.");
            }
            Map<String, Integer> headers = parseHeader(headerLine);
            requireColumn(headers, "ad");
            requireColumn(headers, "soyad");
            requireColumn(headers, "telefon");

            String line;
            int satirNo = 1;
            while ((line = reader.readLine()) != null) {
                satirNo++;
                if (line.isBlank()) continue;
                toplam++;
                String[] cols = line.split(",", -1);
                try {
                    CustomerRequest req = new CustomerRequest();
                    req.setAd(safe(cols, headers.get("ad")));
                    req.setSoyad(safe(cols, headers.get("soyad")));
                    req.setTelefon(safe(cols, headers.get("telefon")));
                    if (headers.containsKey("email")) {
                        String email = safe(cols, headers.get("email"));
                        req.setEmail(email.isEmpty() ? null : email);
                    }
                    if (headers.containsKey("notlar")) {
                        req.setNotlar(safe(cols, headers.get("notlar")));
                    }
                    customerService.create(req);
                    basarili++;
                } catch (ApiException e) {
                    hatalar.add(new CustomerImportResponse.ImportError(satirNo, e.getMessage()));
                } catch (Exception e) {
                    hatalar.add(new CustomerImportResponse.ImportError(satirNo, "Beklenmeyen hata: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("Dosya okunamadi: " + e.getMessage());
        }

        return CustomerImportResponse.builder()
                .toplam(toplam)
                .basarili(basarili)
                .hatalar(hatalar)
                .build();
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        String[] cols = headerLine.split(",", -1);
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            map.put(cols[i].trim().toLowerCase(), i);
        }
        return map;
    }

    private void requireColumn(Map<String, Integer> headers, String key) {
        if (!headers.containsKey(key)) {
            throw ApiException.badRequest("CSV header'inda '" + key + "' kolonu olmalı.");
        }
    }

    private String safe(String[] cols, Integer idx) {
        if (idx == null || idx >= cols.length) return "";
        return cols[idx].trim();
    }
}
