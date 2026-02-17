package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponseDTO(
        UUID id,
        String description,
        BigDecimal amount,
        LocalDate date,
        CategoryResponseDTO category
) {
    public static TransactionResponseDTO fromTransaction(Transaction transaction) {
        return new TransactionResponseDTO(
            transaction.getId(),
            transaction.getDescription(),
            transaction.getAmount(),
            transaction.getDate(),
            CategoryResponseDTO.fromCategory(transaction.getCategory())
        );
    }
}
