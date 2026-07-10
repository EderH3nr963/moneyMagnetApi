package com.moneyMagnetApi.demo.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDTO(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ter um formato válido")
        String email
) {
}
