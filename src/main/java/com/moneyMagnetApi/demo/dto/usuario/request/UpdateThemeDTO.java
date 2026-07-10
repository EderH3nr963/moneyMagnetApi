package com.moneyMagnetApi.demo.dto.usuario.request;

import com.moneyMagnetApi.demo.domain.usuario.UsuarioTheme;
import jakarta.validation.constraints.NotNull;

public record UpdateThemeDTO(
        @NotNull(message = "O tema e obrigatorio")
        UsuarioTheme theme
) {
}
