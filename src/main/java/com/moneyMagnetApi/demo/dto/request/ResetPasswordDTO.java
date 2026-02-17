package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 17, message = "A nova senha deve ter entre 8 e 17 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).*$",
                message = "A nova senha deve conter pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        )
        String password,

        @NotBlank(message = "A confirmação da senha é obrigatória")
        String confirmPassword
) {
}
