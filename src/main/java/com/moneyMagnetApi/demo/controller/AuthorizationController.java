package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.auth.request.ForgotPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.request.LoginRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.RegisterRequestDTO;
import com.moneyMagnetApi.demo.dto.auth.request.ResetPasswordDTO;
import com.moneyMagnetApi.demo.dto.auth.response.AuthorizationResponseDTO;
import com.moneyMagnetApi.demo.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autorizacao", description = "Cadastro, login e recuperacao de senha")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
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
            @RequestBody RegisterRequestDTO dto
    ) {
        AuthorizationResponseDTO response = authorizationService.register(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @RequestBody LoginRequestDTO dto
    ) {
        AuthorizationResponseDTO response = authorizationService.login(dto);

        return ResponseEntity.ok().body(response);
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
            @RequestBody ForgotPasswordDTO dto
    ) {
        authorizationService.forgotPassword(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password/{token}")
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
            @RequestBody ResetPasswordDTO dto,
            @Parameter(description = "Token de redefinicao recebido por e-mail") @PathVariable String token
    ) {
        authorizationService.resetPassword(token, dto);

        return ResponseEntity.noContent().build();
    }
}
