package com.moneyMagnetApi.demo.dto.dashboard.response;

import java.math.BigDecimal;

public record MonthlyFinancialResponse(
        int year,
        int month,
        String label,
        BigDecimal income,
        BigDecimal expenses
) {
}
