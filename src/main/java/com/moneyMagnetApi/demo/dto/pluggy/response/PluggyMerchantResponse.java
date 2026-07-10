package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyMerchantResponse(
        String name,
        String businessName,
        String cnpj,
        String cnae,
        String category
) {
}