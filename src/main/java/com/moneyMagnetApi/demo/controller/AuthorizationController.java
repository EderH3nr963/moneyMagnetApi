package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.request.ForgotPasswordDTO;
import com.moneyMagnetApi.demo.dto.request.LoginRequestDTO;
import com.moneyMagnetApi.demo.dto.request.RegisterRequestDTO;
import com.moneyMagnetApi.demo.dto.request.ResetPasswordDTO;
import com.moneyMagnetApi.demo.dto.response.AuthorizationResponseDTO;
import com.moneyMagnetApi.demo.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name="Autorizacao",
        description = "Rotas para autorização e cadastro"
)
public class AuthorizationController {
    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            description = "Cadastro de usuário",
            operationId = "usuarioRegister"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthorizationResponseDTO> register(
            @RequestBody RegisterRequestDTO dto
    ) {
        AuthorizationResponseDTO response = authorizationService.register(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            description = "Login de usuário",
            operationId = "usuarioLogin"
    )
    public ResponseEntity<AuthorizationResponseDTO> login(
            @RequestBody LoginRequestDTO dto
    ) {
        AuthorizationResponseDTO response = authorizationService.login(dto);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    public ResponseEntity<Void> forgotPassword(
            @RequestBody ForgotPasswordDTO dto
    ) {
        authorizationService.forgotPassword(dto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password/{token}")
    @SecurityRequirements
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordDTO dto,
            @PathVariable String token
    ) {
        authorizationService.resetPassword(token, dto);

        return ResponseEntity.noContent().build();
    }
}
