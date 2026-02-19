package com.moneyMagnetApi.demo.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal receita,
        BigDecimal despesa,
        BigDecimal lucro,
        Double margem
) {}
