package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.usuario.PasswordResetToken;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;
import com.moneyMagnetApi.demo.dto.auth.request.ForgotPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.request.LoginRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.RegisterRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.ResetPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.response.AuthorizationResponseDTO;
import com.moneyMagnetApi.demo.dto.usuario.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.repository.*;
import com.moneyMagnetApi.demo.security.TokenService;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.utils.TokenGenerator;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {
    @Value("${app.frontend.base-url}")
    private String baseFrontUrl;

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final ResourceAuthorizationService resourceAuthorizationService;
    private final TokenHashService tokenHashService;

    private AuthenticatedSession authenticateAndGenerateResponse(
            String email,
            String password
    ) {
        var authToken = new UsernamePasswordAuthenticationToken(email, password);
        var auth = authenticationManager.authenticate(authToken);

        UsuarioDetailsImpl details = (UsuarioDetailsImpl) auth.getPrincipal();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        String token = tokenService.generateToken(details, expiresAt);

        Usuario usuario = details.getUsuario();
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(usuario);
        return new AuthenticatedSession(
                new AuthorizationResponseDTO(expiresAt, token, UsuarioResponseDTO.fromUsuario(usuario)),
                refreshToken.rawToken(),
                refreshToken.expiresAt()
        );
    }

    public AuthenticatedSession login(LoginRequestDTO dto) {
        return authenticateAndGenerateResponse(dto.email(), dto.password());
    }

    @Transactional
    public AuthenticatedSession register(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Email já cadastrado", HttpStatus.CONFLICT);
        }
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("Nome de usuário já cadastrado", HttpStatus.CONFLICT);
        }
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BusinessException("A senha e a confirmação não coincidem", HttpStatus.CONFLICT);
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.email());
        usuario.setUsername(dto.username());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRole(UsuarioRole.USER);

        usuarioRepository.save(usuario);

        return authenticateAndGenerateResponse(dto.email(), dto.password());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElse(null);
        
        if  (usuario == null) {
            return;
        }
        
        tokenRepository.deleteByUsuario(usuario);

        String token = TokenGenerator.generateToken(32);
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHashService.hash(token));
        resetToken.setUsuario(usuario);
        resetToken.setExpiresAt(expiresAt);
        tokenRepository.save(resetToken);

        String resetLink = baseFrontUrl + "/auth/reset-password#token=" + token;

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("templates/reset-password.html")) {

            if (inputStream == null) {
                throw new RuntimeException("Template reset-password.html não encontrado.");
            }

            String htmlBody = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            htmlBody = htmlBody
                    .replace("{{username}}", usuario.getUsername())
                    .replace("{{reset_uri}}", resetLink);

            emailService.sendHtmlEmail(
                    usuario.getEmail(),
                    "Redefinição de Senha",
                    htmlBody
            );

        } catch (IOException | MessagingException e) {
            throw new RuntimeException("Erro ao enviar email de redefinição de senha", e);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHashService.hash(dto.token()))
                .orElseThrow(() -> new EntityNotFoundException("Token inválido ou expirado!"));

        // Validações
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Token inválido ou expirado!");
        }

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new ValidationException("A nova senha e a confirmação não coincidem");
        }

        if (resetToken.getUsedAt() != null) {
            throw new ValidationException("Token já utilizado");
        }

        // Atualiza a senha do usuário
        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        refreshTokenService.revokeAll(usuario);
        usuarioRepository.save(usuario);

        // Marca o token como usado
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);
    }

    @Transactional
    public AuthenticatedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException("Refresh token ausente.", HttpStatus.UNAUTHORIZED);
        }
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        Usuario usuario = rotated.usuario();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        String accessToken = tokenService.generateToken(new UsuarioDetailsImpl(usuario), expiresAt);
        
        return new AuthenticatedSession(
                new AuthorizationResponseDTO(
                        expiresAt,
                        accessToken,
                        UsuarioResponseDTO.fromUsuario(usuario)
                ),
                rotated.rawToken(),
                rotated.expiresAt()
        );
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public record AuthenticatedSession(
            AuthorizationResponseDTO authorization,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {}
}
