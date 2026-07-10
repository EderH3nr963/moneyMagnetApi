package com.moneyMagnetApi.demo.dto.dashboard.response;

import java.math.BigDecimal;

public record DashboardMetricResponse(
        BigDecimal amount,
        BigDecimal percentageChange
) {
}
