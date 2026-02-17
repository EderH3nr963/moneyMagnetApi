package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateAmountTransactionDTO(

        @NotNull(message = "O valor da transação é obrigatório")
        @DecimalMin(value = "0.01", inclusive = true, message = "O valor deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "O valor deve ter no máximo 12 dígitos inteiros e 2 decimais")
        BigDecimal amount

) {}
