package com.moneyMagnetApi.demo.dto.response;

import java.math.BigDecimal;

public record MonthlyFinancialSummaryDTO(
        Integer month,
        Integer year,
        BigDecimal receita,
        BigDecimal despesa
) {

    public BigDecimal lucro() {
        return receita.subtract(despesa);
    }
}
