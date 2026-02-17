package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequestDTO(

        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 17, message = "O nome de usuário deve ter entre 3 e 17 caracteres")
        String username,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve possuir um formato válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,17}$",
                message = "A senha deve ter entre 8 e 17 caracteres, incluindo letra maiúscula, minúscula, número e caractere especial"
        )
        String password,

        @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmPassword

) {}
