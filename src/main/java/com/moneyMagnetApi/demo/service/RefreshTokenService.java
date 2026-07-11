package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.usuario.RefreshToken;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.repository.RefreshTokenRepository;
import com.moneyMagnetApi.demo.utils.TokenGenerator;
import com.moneyMagnetApi.demo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.refresh-token.expiration-days:30}")
    private long expirationDays;

    @Transactional
    public IssuedRefreshToken issue(Usuario usuario) {
        String rawToken = TokenGenerator.generateToken(48);
        Instant expiresAt = Instant.now().plus(expirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenVersion(usuario.getTokenVersion());
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException("Refresh token invalido.", HttpStatus.UNAUTHORIZED));

        Usuario usuario = current.getUsuario();
        if (current.getExpiresAt().isBefore(Instant.now())
                || current.getTokenVersion() != usuario.getTokenVersion()) {
            refreshTokenRepository.delete(current);
            throw new BusinessException("Refresh token invalido ou expirado.", HttpStatus.UNAUTHORIZED);
        }

        refreshTokenRepository.delete(current);
        refreshTokenRepository.flush();
        IssuedRefreshToken replacement = issue(usuario);
        return new RotatedRefreshToken(usuario, replacement.rawToken(), replacement.expiresAt());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeAll(Usuario usuario) {
        refreshTokenRepository.deleteAllByUsuario(usuario);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }

    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {}
    public record RotatedRefreshToken(Usuario usuario, String rawToken, Instant expiresAt) {}
}
