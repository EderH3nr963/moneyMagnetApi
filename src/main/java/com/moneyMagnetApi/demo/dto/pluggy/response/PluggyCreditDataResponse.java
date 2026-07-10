package com.moneyMagnetApi.demo.dto.pluggy.response;

import java.math.BigDecimal;

public record PluggyCreditDataResponse(
        String level,
        String brand,
        String balanceCloseDate,
        String balanceDueDate,
        BigDecimal availableCreditLimit,
        BigDecimal balanceForeignCurrency,
        BigDecimal minimumPayment,
        BigDecimal creditLimit,
        String status,
        String holderType
) {
}