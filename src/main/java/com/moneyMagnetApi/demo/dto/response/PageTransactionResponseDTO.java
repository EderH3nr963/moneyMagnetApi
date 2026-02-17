package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageTransactionResponseDTO(
        List<TransactionResponseDTO> transactions,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static PageTransactionResponseDTO from(Page<Transaction> page) {
        return new PageTransactionResponseDTO(
                page.getContent().stream()
                        .map(TransactionResponseDTO::fromTransaction)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
