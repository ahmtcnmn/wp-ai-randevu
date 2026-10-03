package com.appointflow.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class CalismaSaatiRequest {
    private Long uzmanId;
    private Integer gunOfWeek;
    private LocalTime baslangicSaat;
    private LocalTime bitisSaat;
    private Boolean kapaliMi;
}
