package com.moneyMagnetApi.demo.dto.request;

import com.moneyMagnetApi.demo.domain.category.CategoryType;
import jakarta.validation.constraints.NotNull;

public record UpdateTypeCategoryDTO(

        @NotNull(message = "O tipo da categoria é obrigatório")
        CategoryType type

) {}
