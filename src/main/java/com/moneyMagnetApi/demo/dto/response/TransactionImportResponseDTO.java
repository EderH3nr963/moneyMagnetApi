package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;

import java.util.List;

public record TransactionImportResponseDTO (
        int validTransaction,
        List<TransactionResponseDTO> transactions,
        List<String> errors
) {}
