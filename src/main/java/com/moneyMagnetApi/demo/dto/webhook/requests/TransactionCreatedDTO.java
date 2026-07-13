package com.moneyMagnetApi.demo.dto.webhook.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record TransactionCreatedDTO(
        @NotBlank
        @Pattern(regexp = "transactions/created")
        String event,
        @NotBlank String eventId,
        @NotBlank String itemId,
        @NotBlank String accountId,
        @PositiveOrZero Integer transactionsCount,
        Instant transactionsMinDate,
        Instant transactionsCreatedAtFrom,
        String createdTransactionsLink
) {
}
