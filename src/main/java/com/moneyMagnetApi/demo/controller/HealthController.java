package com.moneyMagnetApi.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Verificacao de disponibilidade da API")
public class HealthController {

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(
            summary = "Verifica se a API esta online",
            responses = {
                    @ApiResponse(responseCode = "200", description = "API disponivel")
            }
    )
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
