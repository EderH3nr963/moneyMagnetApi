package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEmailAndUsernameDTO(
        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 17, message = "O nome de usuário deve ter entre 3 e 17 caracteres")
        String username,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve possuir um formato válido")
        String email
) {
}
