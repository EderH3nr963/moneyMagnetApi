package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionSyncServiceTest {

    private final TransactionSyncService service =
            new TransactionSyncService(null, null, null, null, null);

    @Test
    void shouldUseNatureReceivedFromPluggy() {
        Account account = account(AccountType.CHECKING);

        TransactionNature nature = service.resolveNature(
                transaction("100.00", "CREDIT", "REFUND", "08090000"),
                account
        );

        assertThat(nature).isEqualTo(TransactionNature.REFUND);
    }

    @Test
    void shouldIdentifyCreditCardDebtReductionWhenNatureIsMissing() {
        Account account = account(AccountType.CREDIT);

        TransactionNature nature = service.resolveNature(
                transaction("-500.00", "CREDIT", null, "05100000"),
                account
        );

        assertThat(nature).isEqualTo(TransactionNature.CREDIT_CARD_PAYMENT);
    }

    @Test
    void shouldClassifyCreditCardPurchaseAsExpenseWhenNatureIsMissing() {
        Account account = account(AccountType.CREDIT);

        TransactionNature nature = service.resolveNature(
                transaction("75.00", "DEBIT", null, "11010000"),
                account
        );

        assertThat(nature).isEqualTo(TransactionNature.EXPENSE);
    }

    @Test
    void shouldShowCardPaymentAsExpenseOnCheckingAccount() {
        Account account = account(AccountType.CHECKING);

        TransactionNature nature = service.resolveNature(
                transaction("-500.00", "DEBIT", "CREDIT_CARD_PAYMENT", "05100000"),
                account
        );

        assertThat(nature).isEqualTo(TransactionNature.EXPENSE);
    }

    private Account account(AccountType type) {
        Account account = new Account();
        account.setType(type);
        return account;
    }

    private PluggyTransactionResponse transaction(
            String amount,
            String type,
            String nature,
            String categoryId
    ) {
        return new PluggyTransactionResponse(
                "transaction-id",
                "Transaction",
                null,
                "BRL",
                new BigDecimal(amount),
                null,
                null,
                null,
                null,
                categoryId,
                null,
                null,
                null,
                null,
                type,
                nature,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
