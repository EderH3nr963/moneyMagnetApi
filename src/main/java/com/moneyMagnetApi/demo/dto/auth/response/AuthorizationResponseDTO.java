package com.moneyMagnetApi.demo.dto.auth.response;

import com.moneyMagnetApi.demo.dto.usuario.response.UsuarioResponseDTO;

import java.time.Instant;

public record AuthorizationResponseDTO(
        Instant expiration,
        String token,
        UsuarioResponseDTO usuario
)
{ }
