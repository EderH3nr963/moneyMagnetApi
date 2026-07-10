package com.moneyMagnetApi.demo.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateMerchantCategoryRuleRequest(
        @NotBlank(message = "O merchant e obrigatorio")
        @Size(max = 160, message = "O merchant deve ter no maximo 160 caracteres")
        String merchant,

        @NotNull(message = "A categoria e obrigatoria")
        UUID categoryId
) {
}
