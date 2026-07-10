package com.moneyMagnetApi.demo.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "A cor da categoria é obrigatória")
        @Pattern(
                regexp = "^#(?:[A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$",
                message = "A cor deve estar no formato hexadecimal, por exemplo #FFF ou #FF00FF"
        )
        String color,

        @Size(max = 50, message = "O ícone deve ter no máximo 50 caracteres")
        String icon
) {
}
