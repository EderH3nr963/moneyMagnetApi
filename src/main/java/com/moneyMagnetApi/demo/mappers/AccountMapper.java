package com.moneyMagnetApi.demo.mappers;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountsResponse;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public class AccountMapper {
    public static Account toEntity(Account account, PluggyAccountResponse response, Item item, LocalDateTime syncedAt) {
        account.setItem(item);
        account.setName(resolveName(response));
        account.setType(resolveType(response));
        account.setSubtype(response.subtype());
        account.setCurrency(resolveCurrency(response.currencyCode()));
        account.setBalance(response.balance() == null ? BigDecimal.ZERO : response.balance());
        account.setCreditLimit(response.creditData() == null ? null : response.creditData().creditLimit());
        account.setNumber(resolveNumber(response));
        account.setLastAccountSync(syncedAt);
        
        return account;
    }
    
     public static String resolveName(PluggyAccountResponse response) {
        if (StringUtils.hasText(response.marketingName())) {
            return response.marketingName().trim();
        }
        if (StringUtils.hasText(response.name())) {
            return response.name().trim();
        }
        return "Conta";
    }
    
    public static AccountType resolveType(PluggyAccountResponse response) {
        String value = ((response.type() == null ? "" : response.type()) + " "
                + (response.subtype() == null ? "" : response.subtype()))
                .toUpperCase(Locale.ROOT);
        
        if (value.contains("CHECKING") || value.contains("CONTA_CORRENTE")) return com.moneyMagnetApi.demo.domain.account.AccountType.CHECKING;
        if (value.contains("SAVING") || value.contains("POUPANCA")) return AccountType.SAVINGS;
        if (value.contains("CREDIT") || value.contains("CARTAO")) return AccountType.CREDIT;
        if (value.contains("LOAN") || value.contains("EMPRESTIMO")) return AccountType.LOAN;
        if (value.contains("INVEST")) return AccountType.INVESTMENT;
        if (value.contains("WALLET")) return AccountType.WALLET;
        if (value.contains("PREPAID")) return AccountType.PREPAID;
        return AccountType.OTHER;
    }
    
    public static String  resolveCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "BRL";
    }
    
    public static String resolveNumber(PluggyAccountResponse response) {
        if (StringUtils.hasText(response.number())) {
            return response.number().trim();
        }
        if (response.bankData() != null && StringUtils.hasText(response.bankData().transferNumber())) {
            return response.bankData().transferNumber().trim();
        }
        return null;
    }
}
