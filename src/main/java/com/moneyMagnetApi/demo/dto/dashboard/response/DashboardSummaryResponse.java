package com.moneyMagnetApi.demo.dto.dashboard.response;

public record DashboardSummaryResponse(
        DashboardMetricResponse totalBalance,
        DashboardMetricResponse income,
        DashboardMetricResponse expenses
) {
}
