package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.item.request.CreateItemRequest;
import com.moneyMagnetApi.demo.dto.item.response.ItemSyncResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Conexoes bancarias criadas pela Pluggy")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra e sincroniza um Item",
            description = "Valida o Item na Pluggy, salva a conexao e sincroniza contas e transacoes.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Item registrado e sincronizado"),
                    @ApiResponse(responseCode = "409", description = "Item ainda nao sincronizavel"),
                    @ApiResponse(responseCode = "401", description = "Token JWT ausente ou invalido"),
                    @ApiResponse(responseCode = "502", description = "Falha de comunicacao com a Pluggy")
            }
    )
    public ResponseEntity<ItemSyncResponse> create(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Valid @RequestBody CreateItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createAndSync(usuarioDetails.getId(), request));
    }
}
