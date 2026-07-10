package com.moneyMagnetApi.demo.dto.dashboard.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryExpenseResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String color
) {
}
