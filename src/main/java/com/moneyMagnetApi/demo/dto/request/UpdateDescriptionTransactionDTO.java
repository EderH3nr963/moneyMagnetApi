package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDescriptionTransactionDTO(

        @NotBlank(message = "A descrição da transação é obrigatória")
        @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
        String description

) {}
