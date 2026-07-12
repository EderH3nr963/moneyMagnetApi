package com.moneyMagnetApi.demo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moneyMagnetApi.demo.domain.usuario.RefreshToken;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void shouldKeepOriginalRefreshTokenWhenRefreshingAccessToken() {
        Usuario usuario = new Usuario();
        usuario.setTokenVersion(1L);

        RefreshToken currentRefreshToken = new RefreshToken();
        currentRefreshToken.setUsuario(usuario);
        currentRefreshToken.setTokenVersion(1L);
        currentRefreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        currentRefreshToken.setTokenHash("hash");

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(currentRefreshToken));

        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate("raw-token");

        assertThat(rotated.usuario()).isEqualTo(usuario);
        assertThat(rotated.rawToken()).isEqualTo("raw-token");
        assertThat(rotated.expiresAt()).isEqualTo(currentRefreshToken.getExpiresAt());

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
