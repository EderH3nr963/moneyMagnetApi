package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyCreditCardMetadataResponse(
        Integer installmentNumber,
        Integer totalInstallments,
        BigDecimal totalAmount,
        Integer payeeMCC,
        String cardNumber,
        String billId,
        String feeType,
        String feeTypeAdditionalInfo,
        String otherCreditsType,
        String otherCreditsAdditionalInfo
) {
}