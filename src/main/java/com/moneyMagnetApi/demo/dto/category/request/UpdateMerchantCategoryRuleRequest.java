package com.moneyMagnetApi.demo.dto.category.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMerchantCategoryRuleRequest(
        @NotNull(message = "A categoria e obrigatoria")
        UUID categoryId,

        Boolean active
) {
}
