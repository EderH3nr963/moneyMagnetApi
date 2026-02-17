package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.usuario.PasswordResetToken;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UsuarioRole;
import com.moneyMagnetApi.demo.dto.request.ForgotPasswordDTO;
import com.moneyMagnetApi.demo.dto.request.LoginRequestDTO;
import com.moneyMagnetApi.demo.dto.request.RegisterRequestDTO;
import com.moneyMagnetApi.demo.dto.request.ResetPasswordDTO;
import com.moneyMagnetApi.demo.dto.response.AuthorizationResponseDTO;
import com.moneyMagnetApi.demo.dto.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.repository.PasswordResetTokenRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import com.moneyMagnetApi.demo.security.TokenService;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.utils.TokenGenerator;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthorizationService {
    @Value("${app.frontend.base-url}")
    private String baseFrontUrl;

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository; // ✅ ponto e vírgula
    private final EmailService emailService;

    public AuthorizationService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    private AuthorizationResponseDTO authenticateAndGenerateResponse(
            String email,
            String password
    ) {
        var authToken = new UsernamePasswordAuthenticationToken(email, password);
        var auth = authenticationManager.authenticate(authToken);

        UsuarioDetailsImpl details = (UsuarioDetailsImpl) auth.getPrincipal();
        Instant expiresAt = Instant.now().plusMillis(1000 * 60 * 60 * 48); // 48h
        String token = tokenService.generateToken(details, expiresAt);

        Usuario usuario = details.getUsuario();
        return new AuthorizationResponseDTO(expiresAt ,token, UsuarioResponseDTO.fromUsuario(usuario));
    }

    public AuthorizationResponseDTO login(LoginRequestDTO dto) {
        return authenticateAndGenerateResponse(dto.email(), dto.password());
    }

    @Transactional
    public AuthorizationResponseDTO register(RegisterRequestDTO dto) {
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

        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        tokenRepository.deleteByUsuario(usuario);

        String token = TokenGenerator.generateToken(32);
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUsuario(usuario);
        resetToken.setExpiresAt(expiresAt);
        tokenRepository.save(resetToken);

        String resetLink = baseFrontUrl + "/reset-password#token=" + token;

        System.out.println("Enviar email para " + usuario.getEmail() + " com link: " + resetLink);

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
    public void resetPassword(String token, ResetPasswordDTO dto) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
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
        usuarioRepository.save(usuario);

        // Marca o token como usado
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);
    }


}
