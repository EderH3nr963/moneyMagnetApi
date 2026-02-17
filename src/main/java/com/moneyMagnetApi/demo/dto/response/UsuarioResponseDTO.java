package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String email,
        String username,
        LocalDateTime createdAt,
        UsuarioRole role
) {
    public static UsuarioResponseDTO fromUsuario(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getUsername(),
                usuario.getCreatedAt(),
                usuario.getRole()
        );
    }
}
