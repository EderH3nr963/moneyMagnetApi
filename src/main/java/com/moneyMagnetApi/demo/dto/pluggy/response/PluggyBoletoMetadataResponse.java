package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyBoletoMetadataResponse(
        String digitableLine,
        String barcode,
        BigDecimal baseAmount,
        BigDecimal interestAmount,
        BigDecimal penaltyAmount,
        BigDecimal discountAmount
) {
}