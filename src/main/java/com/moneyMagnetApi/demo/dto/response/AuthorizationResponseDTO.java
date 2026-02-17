package com.moneyMagnetApi.demo.dto.response;

import java.time.Instant;

public record AuthorizationResponseDTO(
        Instant expiration,
        String token,
        UsuarioResponseDTO usuario
)
{ }
