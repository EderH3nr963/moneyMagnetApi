package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record UpdateDateTransactionDTO(

        @NotNull(message = "A data da transação é obrigatória")
        @PastOrPresent(message = "A data da transação não pode estar no futuro")
        LocalDate date

) {}
