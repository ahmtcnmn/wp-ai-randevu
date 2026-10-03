package com.appointflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TwoFactorEnableResponse {
    private List<String> recoveryCodes;
}
