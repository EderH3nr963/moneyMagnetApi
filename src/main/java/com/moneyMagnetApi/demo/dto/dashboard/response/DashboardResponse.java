package com.moneyMagnetApi.demo.dto.dashboard.response;

import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;

import java.time.YearMonth;
import java.util.List;

public record DashboardResponse(
        YearMonth referenceMonth,
        DashboardSummaryResponse summary,
        List<MonthlyFinancialResponse> financialHistory,
        List<CategoryExpenseResponse> expensesByCategory,
        List<AccountResponse> linkedAccounts,
        List<TransactionResponse> recentTransactions
) {
}
