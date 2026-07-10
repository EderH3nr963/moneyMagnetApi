package com.moneyMagnetApi.demo.dto.transaction.response;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.category.response.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        
        UUID id,
        
        BigDecimal amount,
        
        String description,
        
        String merchant,
        
        LocalDateTime date,
        
        LocalDateTime paymentDate,
        
        String currency,
        
        TransactionStatus status,
        
        TransactionType type,

        TransactionNature nature,
        
        UUID accountId,
        
        String accountName,
        
        CategoryResponse category

) {
    
    public static TransactionResponse fromResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getMerchant(),
                transaction.getDate(),
                transaction.getPaymentDate(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getType(),
                transaction.getNature(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                CategoryResponse.fromResponse(transaction.getCategory())
        );
    }
}
