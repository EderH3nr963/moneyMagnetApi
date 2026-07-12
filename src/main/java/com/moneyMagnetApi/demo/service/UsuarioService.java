package com.moneyMagnetApi.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.domain.usuario.UpdateEmail;
import com.moneyMagnetApi.demo.dto.usuario.request.ConfirmEmailDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdateEmailAndUsernameDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdateEmailDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdatePasswordDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdateThemeDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdateUsernameDTO;
import com.moneyMagnetApi.demo.dto.usuario.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import com.moneyMagnetApi.demo.repository.UpdateEmailRepository;
import com.moneyMagnetApi.demo.utils.TokenGenerator;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;

@Service
public class UsuarioService {

    @Value("${app.frontend.base-url}")
    private String baseFrontUrl;

    private final UsuarioRepository usuarioRepository;
    private final UpdateEmailRepository updateEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            UpdateEmailRepository updateEmailRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            EmailService emailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.updateEmailRepository = updateEmailRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO getById(UUID id) {

        Usuario usuario = findUsuarioOrThrow(id);

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateEmailAndUsername(UUID id, UpdateEmailAndUsernameDTO dto) {
        Usuario usuario = findUsuarioOrThrow(id);
        
        if (!usuario.getEmail().equals(dto.email())) {
            throw new BusinessException("O email informado já está em uso", HttpStatus.CONFLICT);
        }
        
        if (!usuario.getUsername().equals(dto.username())
                && usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("O nome de usuário informado já está em uso", HttpStatus.CONFLICT);
        }

        usuario.setUsername(dto.username());
        requestEmailUpdate(id, new UpdateEmailDTO(dto.email()));

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public void requestEmailUpdate(UUID id, UpdateEmailDTO dto) {
        Usuario usuario = findUsuarioOrThrow(id);

        if (usuario.getEmail().equalsIgnoreCase(dto.email())) {
            throw new BusinessException("O novo e-mail deve ser diferente do atual", HttpStatus.BAD_REQUEST);
        }
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("O e-mail informado já está em uso", HttpStatus.CONFLICT);
        }

        updateEmailRepository.deleteByUsuario(usuario);

        String token = TokenGenerator.generateToken(32);
        UpdateEmail updateEmail = new UpdateEmail();
        updateEmail.setNewEmail(dto.email());
        updateEmail.setTokenHash(hashToken(token));
        updateEmail.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        updateEmail.setUsuario(usuario);
        updateEmailRepository.save(updateEmail);

        String confirmationLink = baseFrontUrl + "/auth/confirm-email#token=" + token;
        sendEmailConfirmation(usuario, dto.email(), confirmationLink);
    }

    @Transactional
    public void confirmEmailUpdate(ConfirmEmailDTO dto) {
        UpdateEmail updateEmail = updateEmailRepository.findByTokenHash(hashToken(dto.token()))
                .orElseThrow(() -> new EntityNotFoundException("Token inválido"));

        if (!passwordEncoder.matches(dto.password(), updateEmail.getUsuario().getPassword())) {
            throw new ValidationException("Senha inválida");
        }
        if (updateEmail.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Token inválido ou expirado");
        }
        if (updateEmail.getUsedAt() != null) {
            throw new ValidationException("Token já utilizado");
        }
        if (usuarioRepository.existsByEmail(updateEmail.getNewEmail())) {
            throw new BusinessException("O e-mail informado já está em uso", HttpStatus.CONFLICT);
        }

        Usuario usuario = updateEmail.getUsuario();
        usuario.setEmail(updateEmail.getNewEmail());
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        refreshTokenService.revokeAll(usuario);
        updateEmail.setUsedAt(Instant.now());
    }

    @Transactional
    public UsuarioResponseDTO updateUsername(UUID id, UpdateUsernameDTO dto) {

        Usuario usuario = findUsuarioOrThrow(id);

        if (!usuario.getUsername().equals(dto.username())
                && usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("O nome de usuário informado já está em uso", HttpStatus.CONFLICT);
        }

        usuario.setUsername(dto.username());

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public void updatePassword(UUID id, UpdatePasswordDTO dto) {

        Usuario usuario = findUsuarioOrThrow(id);

        if (!passwordEncoder.matches(dto.currentPassword(), usuario.getPassword())) {
            throw new ValidationException("A senha atual informada é inválida");
        }

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new ValidationException("A nova senha e a confirmação não coincidem");
        }

        String novaSenhaHash = passwordEncoder.encode(dto.password());
        usuario.setPassword(novaSenhaHash);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        refreshTokenService.revokeAll(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateTheme(UUID id, UpdateThemeDTO dto) {
        Usuario usuario = findUsuarioOrThrow(id);

        usuario.setTheme(dto.theme());

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public void deleteById(UUID id) {

        Usuario usuario = findUsuarioOrThrow(id);

        usuarioRepository.delete(usuario);
    }

    private Usuario findUsuarioOrThrow(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Usuário não encontrado")
                );
    }

    private void sendEmailConfirmation(Usuario usuario, String newEmail, String confirmationLink) {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("templates/confirm-email.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Template confirm-email.html não encontrado");
            }

            String htmlBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{username}}", usuario.getUsername())
                    .replace("{{new_email}}", newEmail)
                    .replace("{{confirm_uri}}", confirmationLink);

            emailService.sendHtmlEmail(newEmail, "Confirme seu novo e-mail", htmlBody);
        } catch (IOException | MessagingException exception) {
            throw new IllegalStateException("Erro ao enviar confirmação de e-mail", exception);
        }
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
