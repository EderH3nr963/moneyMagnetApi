package com.moneyMagnetApi.demo.dto.request;

import com.moneyMagnetApi.demo.domain.category.CategoryType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryDTO(

        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(min = 3, max = 50, message = "O nome da categoria deve ter entre 3 e 50 caracteres")
        String name,

        @NotNull(message = "O tipo da categoria é obrigatório")
        @Enumerated(EnumType.STRING)
        CategoryType type,

        @Pattern(
                regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
                message = "A cor deve estar no formato hexadecimal, ex: #FFF ou #FF00FF"
        )
        String color
) {}
