package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyTransactionResponse(
        String id,
        String description,
        String descriptionRaw,
        String currencyCode,
        BigDecimal amount,
        BigDecimal amountInAccountCurrency,
        Instant date,
        BigDecimal balance,
        String category,
        String categoryId,
        String accountId,
        String providerCode,
        TransactionStatus status,
        PluggyPaymentDataResponse paymentData,
        String type,
        String nature,
        String operationCategory,
        String operationType,
        String operationTypeAdditionalInfo,
        PluggyCreditCardMetadataResponse creditCardMetadata,
        PluggyMerchantResponse merchant,
        String providerId
) {
}
