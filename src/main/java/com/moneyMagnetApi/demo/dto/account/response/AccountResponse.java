package com.moneyMagnetApi.demo.dto.account.response;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID itemId,
        UUID institutionId,
        String institutionName,
        String institutionLogoUrl,
        String institutionPrimaryColor,
        String name,
        AccountType type,
        String subtype,
        String currency,
        BigDecimal balance,
        BigDecimal creditLimit,
        String number,
        LocalDateTime lastTransactionSync,
        LocalDateTime lastAccountSync,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse fromAccount(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getItem().getId(),
                account.getItem().getInstitution().getId(),
                account.getItem().getInstitution().getName(),
                account.getItem().getInstitution().getLogoUrl(),
                account.getItem().getInstitution().getPrimaryColor(),
                account.getName(),
                account.getType(),
                account.getSubtype(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreditLimit(),
                account.getNumber(),
                account.getLastTransactionSync(),
                account.getLastAccountSync(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
