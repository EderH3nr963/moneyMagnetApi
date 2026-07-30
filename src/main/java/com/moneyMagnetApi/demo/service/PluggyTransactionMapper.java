package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Component
public class PluggyTransactionMapper {

    public void fill(Transaction target, Account account, Category category, PluggyTransactionResponse source) {
        target.setPluggyTransactionId(source.id());
        target.setAccount(account);
        target.setDescription(resolveDescription(source));
        target.setAmount(resolveAmount(source));
        target.setCurrency(resolveCurrency(source, account));
        target.setType(resolveType(source));
        target.setStatus(source.status() == null ? TransactionStatus.POSTED : source.status());
        LocalDateTime date = resolveDate(source);
        target.setDate(date);
        target.setPaymentDate(date);
        target.setProviderCategory(source.category());
        target.setProviderCode(source.providerCode());
        target.setMerchant(resolveMerchant(source));
        target.setNature(resolveNature(source, account));
        target.setCategory(category);
    }

    public TransactionNature resolveNature(PluggyTransactionResponse source, Account account) {
        if (StringUtils.hasText(source.nature())) {
            String normalized = source.nature().trim().replace('-', '_').replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
            try {
                TransactionNature received = TransactionNature.valueOf(normalized);
                if (received != TransactionNature.CREDIT_CARD_PAYMENT || account.getType() == AccountType.CREDIT) {
                    return received;
                }
            } catch (IllegalArgumentException ignored) {
                // Usa as regras de compatibilidade para valores novos.
            }
        }
        if ("05100000".equals(source.categoryId()) && account.getType() == AccountType.CREDIT) {
            return TransactionNature.CREDIT_CARD_PAYMENT;
        }
        if (source.categoryId() != null && source.categoryId().startsWith("040")) {
            return TransactionNature.INTERNAL_TRANSFER;
        }
        if (source.categoryId() != null && source.categoryId().startsWith("010")) {
            return TransactionNature.INCOME;
        }
        if (account.getType() == AccountType.CREDIT) {
            return source.amount() != null && source.amount().compareTo(BigDecimal.ZERO) < 0
                    ? TransactionNature.CREDIT_CARD_PAYMENT : TransactionNature.EXPENSE;
        }
        return resolveType(source) == TransactionType.CREDIT
                ? TransactionNature.INCOME : TransactionNature.EXPENSE;
    }

    public String resolveMerchant(PluggyTransactionResponse source) {
        return source.merchant() == null ? null : source.merchant().name();
    }

    private String resolveDescription(PluggyTransactionResponse source) {
        return StringUtils.hasText(source.description()) ? source.description().trim() : "Transacao Pluggy";
    }

    private BigDecimal resolveAmount(PluggyTransactionResponse source) {
        if (source.amount() == null) {
            throw new IllegalArgumentException("Transacao sem valor: " + source.id());
        }
        return source.amount();
    }

    private String resolveCurrency(PluggyTransactionResponse source, Account account) {
        if (StringUtils.hasText(source.currencyCode())) {
            return source.currencyCode().trim().toUpperCase(Locale.ROOT);
        }
        if (StringUtils.hasText(account.getCurrency())) {
            return account.getCurrency();
        }
        throw new IllegalArgumentException("Transacao sem moeda configurada: " + source.id());
    }

    private LocalDateTime resolveDate(PluggyTransactionResponse source) {
        if (source.date() == null) {
            throw new IllegalArgumentException("Transacao sem data: " + source.id());
        }
        return source.date().atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private TransactionType resolveType(PluggyTransactionResponse source) {
        if (StringUtils.hasText(source.type())) {
            try {
                return TransactionType.valueOf(source.type().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Deduz pelo valor.
            }
        }
        return source.amount() != null && source.amount().compareTo(BigDecimal.ZERO) < 0
                ? TransactionType.DEBIT : TransactionType.CREDIT;
    }
}
