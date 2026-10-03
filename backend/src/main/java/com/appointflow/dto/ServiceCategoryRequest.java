package com.appointflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServiceCategoryRequest {
    @NotBlank
    private String ad;
    private String aciklama;
    private String takvimRengi;
}
