package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNameCategoryDTO(

        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(min = 3, max = 50, message = "O nome da categoria deve ter entre 3 e 50 caracteres")
        String name

) {}
