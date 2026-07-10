package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Contas", description = "Consulta e sincronizacao de contas financeiras")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(
            summary = "Lista contas do usuario",
            description = "Retorna as contas financeiras sincronizadas para o usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contas retornadas"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<List<AccountResponse>> findAll(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        return ResponseEntity.ok(accountService.findAll(usuarioDetails.getId()));
    }

    @GetMapping("/{accountId}")
    @Operation(
            summary = "Busca uma conta por ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Conta encontrada"),
                    @ApiResponse(responseCode = "404", description = "Conta nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<AccountResponse> findById(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da conta") @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(accountService.findById(usuarioDetails.getId(), accountId));
    }

    @GetMapping("/item/{itemId}")
    @Operation(
            summary = "Lista contas de um Item Pluggy",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contas retornadas"),
                    @ApiResponse(responseCode = "404", description = "Item nao encontrado"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<List<AccountResponse>> findByItem(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID interno do Item") @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(accountService.findByItem(usuarioDetails.getId(), itemId));
    }
    
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove uma conta",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Conta removida"),
                    @ApiResponse(responseCode = "404", description = "Conta nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public void delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da conta") @PathVariable UUID accountId
    ) {
        accountService.delete(usuarioDetails.getId(), accountId);
    }
}
