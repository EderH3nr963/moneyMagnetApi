package com.moneyMagnetApi.demo.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moneyMagnetApi.demo.dto.auth.request.ForgotPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.request.LoginRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.RegisterRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.ResetPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.response.AuthorizationResponseDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.ConfirmEmailDTO;
import com.moneyMagnetApi.demo.service.AuthorizationService;
import com.moneyMagnetApi.demo.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autorizacao", description = "Cadastro, login e recuperacao de senha")
public class AuthorizationController {

    private static final String REFRESH_COOKIE = "money_magnet_refresh";

    private final AuthorizationService authorizationService;
    private final UsuarioService usuarioService;

    @Value("${api.security.refresh-token.cookie-secure:true}")
    private boolean secureCookie;

    public AuthorizationController(AuthorizationService authorizationService, UsuarioService usuarioService) {
        this.authorizationService = authorizationService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastra um usuario",
            description = "Cria um usuario e retorna a sessao autenticada com JWT.",
            operationId = "usuarioRegister",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuario cadastrado"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "409", description = "E-mail ou username ja cadastrado")
            }
    )
    public ResponseEntity<AuthorizationResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto
    ) {
        AuthorizationService.AuthenticatedSession session = authorizationService.register(dto);

        return sessionResponse(session, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Realiza login",
            description = "Autentica e retorna token JWT e dados do usuario.",
            operationId = "usuarioLogin",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login realizado"),
                    @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
            }
    )
    public ResponseEntity<AuthorizationResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {
        AuthorizationService.AuthenticatedSession session = authorizationService.login(dto);

        return sessionResponse(session, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public ResponseEntity<AuthorizationResponseDTO> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        AuthorizationService.AuthenticatedSession session = authorizationService.refresh(refreshToken);
        return sessionResponse(session, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @SecurityRequirements
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        authorizationService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
            summary = "Solicita recuperacao de senha",
            description = "Gera token temporario e envia e-mail com link para redefinicao.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Solicitacao processada"),
                    @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
            }
    )
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordDTO dto
    ) {
        authorizationService.forgotPassword(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
            summary = "Redefine a senha",
            description = "Valida o token enviado por e-mail e grava a nova senha.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Senha redefinida"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "404", description = "Token invalido ou expirado")
            }
    )
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordDTO dto
    ) {
        authorizationService.resetPassword(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm-email")
    @SecurityRequirements
    @Operation(
            summary = "Confirma a alteração do e-mail",
            description = "Valida o token enviado ao novo endereço e efetiva a alteração.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "E-mail atualizado"),
                    @ApiResponse(responseCode = "400", description = "Token expirado ou já utilizado"),
                    @ApiResponse(responseCode = "404", description = "Token inválido"),
                    @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
            }
    )
    public ResponseEntity<Void> confirmEmail(@Valid @RequestBody ConfirmEmailDTO dto) {
        usuarioService.confirmEmailUpdate(dto);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthorizationResponseDTO> sessionResponse(
            AuthorizationService.AuthenticatedSession session,
            HttpStatus status
    ) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.between(Instant.now(), session.refreshTokenExpiresAt()))
                .build();

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.authorization());
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }
}
