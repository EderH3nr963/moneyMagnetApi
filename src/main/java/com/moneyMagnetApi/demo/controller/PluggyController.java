package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectTokenResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.PluggyClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pluggy")
@RequiredArgsConstructor
@Tag(name = "Pluggy", description = "Integracao com o Pluggy Connect")
public class PluggyController {

    private final PluggyClient pluggyClient;

    @PostMapping("/connect-token")
    @Operation(
            summary = "Cria um Connect Token",
            description = "Cria um token temporario para abrir o Pluggy Connect no frontend.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token criado"),
                    @ApiResponse(responseCode = "401", description = "Token JWT ausente ou invalido"),
                    @ApiResponse(responseCode = "502", description = "Falha de comunicacao com a Pluggy")
            }
    )
    public ResponseEntity<PluggyConnectTokenResponse> createConnectToken(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        return ResponseEntity.ok(pluggyClient.createConnectToken(usuarioDetails.getId()));
    }
}
