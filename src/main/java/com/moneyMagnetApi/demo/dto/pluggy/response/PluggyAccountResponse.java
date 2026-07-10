package com.moneyMagnetApi.demo.dto.pluggy.response;

import java.math.BigDecimal;

public record PluggyAccountResponse(
        String id,
        String type,
        String subtype,
        String number,
        String name,
        String marketingName,
        BigDecimal balance,
        String itemId,
        String taxNumber,
        String owner,
        String currencyCode,
        PluggyBankDataResponse bankData,
        PluggyCreditDataResponse creditData
) {
}