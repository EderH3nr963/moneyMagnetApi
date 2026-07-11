package com.moneyMagnetApi.demo.security;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenServiceTest {

    @Test
    void includesSessionVersionInSignedToken() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-with-enough-entropy");

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setTokenVersion(3);

        String token = tokenService.generateToken(
                new UsuarioDetailsImpl(usuario),
                Instant.now().plusSeconds(60)
        );

        TokenService.ValidatedToken validatedToken = tokenService.validateToken(token);
        assertEquals(usuario.getId(), validatedToken.userId());
        assertEquals(3, validatedToken.tokenVersion());
    }
}
