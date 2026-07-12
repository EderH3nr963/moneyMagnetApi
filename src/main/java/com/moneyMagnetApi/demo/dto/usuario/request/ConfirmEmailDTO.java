package com.moneyMagnetApi.demo.dto.usuario.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailDTO(
        @NotBlank(message = "O token é obrigatório")
        String token,
        
        @NotBlank(message = "A senha é obrigatória")
        String password
) {}
