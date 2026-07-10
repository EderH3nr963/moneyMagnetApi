package com.moneyMagnetApi.demo.dto.usuario.response;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioTheme;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String email,
        String username,
        LocalDateTime createdAt,
        UsuarioRole role,
        UsuarioTheme theme
) {
    public static UsuarioResponseDTO fromUsuario(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getUsername(),
                usuario.getCreatedAt(),
                usuario.getRole(),
                usuario.getTheme()
        );
    }
}
