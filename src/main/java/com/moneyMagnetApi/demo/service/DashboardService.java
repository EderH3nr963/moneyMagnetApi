package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.config.CacheConfig;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.CategoryExpenseResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.DashboardMetricResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.DashboardResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.DashboardSummaryResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.MonthlyFinancialResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import com.moneyMagnetApi.demo.repository.projection.MonthlyFinancialProjection;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final Locale PORTUGUESE_BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final int DEFAULT_HISTORY_MONTHS = 12;
    private static final Set<Integer> ALLOWED_HISTORY_MONTHS = Set.of(6, 9, 12);
    private static final List<TransactionNature> DEFAULT_NATURES = List.of(
            TransactionNature.INCOME,
            TransactionNature.EXPENSE
    );

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CacheConfig cacheConfig;
    
    public List<CategoryExpenseResponse> getExpensesByCategory(UUID userId, YearMonth referenceMonth) {
        return categoryRepository.findExpensesByCategory(
                userId,
                referenceMonth.atDay(1).atTime(0, 0),
                referenceMonth.plusMonths(1).atDay(1).atStartOfDay(),
                TransactionNature.EXPENSE,
                TransactionStatus.POSTED
        );
    }

    public DashboardResponse getDashboard(UUID userId, YearMonth referenceMonth) {
        YearMonth firstHistoryMonth = referenceMonth.minusMonths(DEFAULT_HISTORY_MONTHS - 1L);
        
        List<MonthlyFinancialProjection> monthlyFinancialProjections = transactionRepository.findMonthlyFinancialSummary(
                userId,
                firstHistoryMonth.atDay(1).atTime(0, 0),
                referenceMonth.plusMonths(1).atDay(1).atStartOfDay(),
                TransactionType.CREDIT,
                TransactionType.DEBIT
        );
        
        Map<YearMonth, MonthlyAmounts> totalsByMonth = toMonthlyAmountsMap(firstHistoryMonth, DEFAULT_HISTORY_MONTHS,  monthlyFinancialProjections);
        
        
        List<MonthlyFinancialResponse> financialHistory =
                toMonthlyFinancialHistory(totalsByMonth);

        MonthlyAmounts current = totalsByMonth.get(referenceMonth);
        MonthlyAmounts previous = totalsByMonth.getOrDefault(
                referenceMonth.minusMonths(1),
                MonthlyAmounts.empty()
        );
        
        List<AccountResponse> accounts = accountService.findAll(userId);
        BigDecimal totalBalance = accounts.stream()
                .filter(account -> account.type() != AccountType.CREDIT)
                .map(AccountResponse::balance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal openingBalance = totalBalance.subtract(current.balance());
        
        
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                metric(totalBalance, openingBalance),
                metric(current.income(), previous.income()),
                metric(current.expenses(), previous.expenses())
        );
        List<CategoryExpenseResponse> expensesByCategory =
                categoryRepository.findExpensesByCategory(
                        userId,
                        referenceMonth.atDay(1).atTime(0, 0),
                        referenceMonth.plusMonths(1).atDay(1).atStartOfDay(),
                        TransactionNature.EXPENSE,
                        TransactionStatus.POSTED
                );
        
        List<TransactionResponse> recentTransactions = transactionRepository
                .findTop6ByAccountItemUsuarioIdAndNatureInOrderByDateDescCreatedAtDesc(
                        userId,
                        DEFAULT_NATURES
                )
                .stream()
                .map(TransactionResponse::fromResponse)
                .toList();
        
        return new DashboardResponse(
                referenceMonth,
                summary,
                financialHistory,
                expensesByCategory,
                accounts.stream().filter(account -> account.type() != AccountType.CREDIT).toList(),
                recentTransactions
        );
    }

    public List<MonthlyFinancialResponse> getFinancialHistory(
            UUID userId,
            YearMonth referenceMonth,
            int months
    ) {
        validateHistoryMonths(months);
        YearMonth firstHistoryMonth = referenceMonth.minusMonths(months - 1L);
        List<MonthlyFinancialProjection> monthlyFinancialProjections = transactionRepository.findMonthlyFinancialSummary(
                userId,
                firstHistoryMonth.atDay(1).atTime(0, 0),
                referenceMonth.plusMonths(1).atDay(1).atStartOfDay(),
                TransactionType.CREDIT,
                TransactionType.DEBIT
        );
        
        Map<YearMonth, MonthlyAmounts> totalsByMonth =  toMonthlyAmountsMap(firstHistoryMonth, months,  monthlyFinancialProjections);
        
        return toMonthlyFinancialHistory(totalsByMonth);
    }
    
    

    private List<MonthlyFinancialResponse> toMonthlyFinancialHistory(
            Map<YearMonth, MonthlyAmounts> totalsByMonth
    ) {
        return totalsByMonth.entrySet().stream()
                .map(entry -> toMonthlyResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void validateHistoryMonths(int months) {
        if (!ALLOWED_HISTORY_MONTHS.contains(months)) {
            throw new ValidationException("O historico financeiro aceita apenas 6, 9 ou 12 meses.");
        }
    }

    private MonthlyFinancialResponse toMonthlyResponse(
            YearMonth month,
            MonthlyAmounts amounts
    ) {
        String label = month.getMonth()
                .getDisplayName(TextStyle.SHORT, PORTUGUESE_BRAZIL)
                .replace(".", "");
        label = Character.toUpperCase(label.charAt(0)) + label.substring(1);

        return new MonthlyFinancialResponse(
                month.getYear(),
                month.getMonthValue(),
                label,
                amounts.income(),
                amounts.expenses()
        );
    }

    private DashboardMetricResponse metric(BigDecimal current, BigDecimal previous) {
        return new DashboardMetricResponse(current, percentageChange(current, previous));
    }

    private BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : ONE_HUNDRED.multiply(BigDecimal.valueOf(current.signum()));
        }

        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private Map<YearMonth, MonthlyAmounts> toMonthlyAmountsMap(
            YearMonth firstMonth,
            int months,
            List<MonthlyFinancialProjection> projections
    ) {
        Map<YearMonth, MonthlyAmounts> totals = new LinkedHashMap<>();
        
        for (int index = 0; index < months; index++) {
            totals.put(
                    firstMonth.plusMonths(index),
                    MonthlyAmounts.empty()
            );
        }
        
        projections.forEach(projection -> {
            YearMonth month = YearMonth.of(
                    projection.getYear(),
                    projection.getMonth()
            );
            
            totals.put(
                    month,
                    MonthlyAmounts.of(
                            projection.getIncome(),
                            projection.getExpenses()
                    )
            );
        });
        
        return totals;
    }
    
    private record MonthlyAmounts(
            BigDecimal income,
            BigDecimal expenses
    ) {
        static MonthlyAmounts empty() {
            return new MonthlyAmounts(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
        
        static MonthlyAmounts of(
                BigDecimal income,
                BigDecimal expenses
        ) {
            return new MonthlyAmounts(
                    Objects.requireNonNullElse(income, BigDecimal.ZERO),
                    Objects.requireNonNullElse(expenses, BigDecimal.ZERO)
            );
        }
        
        BigDecimal balance() {
            return income.subtract(expenses);
        }
    }
}
