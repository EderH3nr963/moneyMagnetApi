package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionDTO(

        @NotBlank(message = "A descrição da transação é obrigatória")
        @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
        String description,

        @NotNull(message = "A data da transação é obrigatória")
        @PastOrPresent(message = "A data da transação não pode estar no futuro")
        LocalDate date,

        @NotNull(message = "O valor da transação é obrigatório")
        @DecimalMin(value = "0.01", inclusive = true, message = "O valor deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "O valor deve ter no máximo 12 dígitos inteiros e 2 decimais")
        BigDecimal amount,

        @NotNull(message = "A categoria da transação deve ser informada")
        UUID category_id

) {}
