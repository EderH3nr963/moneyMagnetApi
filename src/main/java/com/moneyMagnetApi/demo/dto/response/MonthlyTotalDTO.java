package com.moneyMagnetApi.demo.dto.response;

import java.math.BigDecimal;

public record MonthlyTotalDTO(
        String mes,
        BigDecimal receita,
        BigDecimal despesa,
        BigDecimal lucro
) {}
