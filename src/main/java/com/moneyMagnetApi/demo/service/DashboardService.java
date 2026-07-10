package com.moneyMagnetApi.demo.service;

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
        List<Transaction> historyTransactions = transactionRepository.findAllByUserAndPeriod(
                userId,
                firstHistoryMonth.atDay(1).atTime(0, 0),
                referenceMonth.plusMonths(1).atDay(1).atStartOfDay()
        );

        Map<YearMonth, MonthlyAmounts> totalsByMonth = buildMonthlyTotals(
                firstHistoryMonth,
                DEFAULT_HISTORY_MONTHS,
                historyTransactions
        );
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

    public List<MonthlyFinancialResponse> getFinancialHistoryPublic(
            UUID userId,
            YearMonth referenceMonth,
            int months
    ) {
        validateHistoryMonths(months);
        YearMonth firstHistoryMonth = referenceMonth.minusMonths(months - 1L);
        List<Transaction> historyTransactions = transactionRepository.findAllByUserAndPeriod(
                userId,
                firstHistoryMonth.atDay(1).atTime(0, 0),
                referenceMonth.plusMonths(1).atDay(1).atStartOfDay()
        );

        return getFinancialHistoryPrivate(referenceMonth, historyTransactions, months);
    }
    
    private List<MonthlyFinancialResponse> getFinancialHistoryPrivate(
            YearMonth referenceMonth,
            List<Transaction> historyTransactions,
            int months
    ) {
        validateHistoryMonths(months);
        YearMonth firstHistoryMonth = referenceMonth.minusMonths(months - 1L);

        return toMonthlyFinancialHistory(
                buildMonthlyTotals(firstHistoryMonth, months, historyTransactions)
        );
    }

    private Map<YearMonth, MonthlyAmounts> buildMonthlyTotals(
            YearMonth firstHistoryMonth,
            int months,
            List<Transaction> historyTransactions
    ) {
        Map<YearMonth, MonthlyAmounts> totalsByMonth = getTotalByMoth(firstHistoryMonth, months);

        historyTransactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.POSTED)
                .forEach(transaction -> addToMonth(totalsByMonth, transaction));

        return totalsByMonth;
    }

    private List<MonthlyFinancialResponse> toMonthlyFinancialHistory(
            Map<YearMonth, MonthlyAmounts> totalsByMonth
    ) {
        return totalsByMonth.entrySet().stream()
                .map(entry -> toMonthlyResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
    
    private Map<YearMonth, MonthlyAmounts> getTotalByMoth(
            YearMonth firstHistoryMonth,
            int months
    ) {
        return initializeMonthlyTotals(firstHistoryMonth, months);
    }

    private Map<YearMonth, MonthlyAmounts> initializeMonthlyTotals(
            YearMonth firstMonth,
            int months
    ) {
        Map<YearMonth, MonthlyAmounts> totals = new LinkedHashMap<>();
        for (int index = 0; index < months; index++) {
            totals.put(firstMonth.plusMonths(index), MonthlyAmounts.empty());
        }
        return totals;
    }

    private void validateHistoryMonths(int months) {
        if (!ALLOWED_HISTORY_MONTHS.contains(months)) {
            throw new ValidationException("O historico financeiro aceita apenas 6, 9 ou 12 meses.");
        }
    }

    private void addToMonth(Map<YearMonth, MonthlyAmounts> totalsByMonth, Transaction transaction) {
        YearMonth month = YearMonth.from(transaction.getDate());
        MonthlyAmounts totals = totalsByMonth.get(month);
        if (totals == null) {
            return;
        }

        BigDecimal amount = transaction.getAmount().abs();
        TransactionNature nature = effectiveNature(transaction);
        
        if (nature == TransactionNature.INCOME) {
            totals.addIncome(amount);
        } else if (nature == TransactionNature.EXPENSE) {
            totals.addExpense(amount);
        } else if (nature == TransactionNature.INTERNAL_TRANSFER) {
            if (transaction.getType() == TransactionType.CREDIT) {
                totals.addIncome(amount);
            } else {
                totals.addExpense(amount);
            }
        }
    }

    private TransactionNature effectiveNature(Transaction transaction) {
        if (transaction.getNature() != null) {
            return transaction.getNature();
        }

        if (transaction.getAccount().getType() == AccountType.CREDIT
                && "Credit card payment".equalsIgnoreCase(
                        transaction.getProviderCategory()
                )) {
            return TransactionNature.CREDIT_CARD_PAYMENT;
        }
        if (transaction.getCategory() != null
                && "transferencia-entre-contas".equals(
                        transaction.getCategory().getNormalizedName()
                )) {
            return TransactionNature.INTERNAL_TRANSFER;
        }
        if (transaction.getAccount().getType() == AccountType.CREDIT) {
            return transaction.getAmount().compareTo(BigDecimal.ZERO) < 0
                    ? TransactionNature.CREDIT_CARD_PAYMENT
                    : TransactionNature.EXPENSE;
        }

        return transaction.getType()
                == com.moneyMagnetApi.demo.domain.transaction.TransactionType.CREDIT
                ? TransactionNature.INCOME
                : TransactionNature.EXPENSE;
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

    private static final class MonthlyAmounts {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expenses = BigDecimal.ZERO;

        static MonthlyAmounts empty() {
            return new MonthlyAmounts();
        }

        void addIncome(BigDecimal amount) {
            income = income.add(amount);
        }

        void addExpense(BigDecimal amount) {
            expenses = expenses.add(amount);
        }

        BigDecimal income() {
            return income;
        }

        BigDecimal expenses() {
            return expenses;
        }

        BigDecimal balance() {
            return income.subtract(expenses);
        }
    }
}
