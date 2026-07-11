package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.dto.institution.response.InstitutionProfileResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Tag(name = "Bancos", description = "Conexoes bancarias separadas por Item")
public class BankController {

    private final BankService bankService;

    @GetMapping
    @Operation(summary = "Lista os bancos conectados separados por Item")
    public ResponseEntity<List<InstitutionProfileResponse>> findAll(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        return ResponseEntity.ok(bankService.findAll(usuarioDetails.getId()));
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Busca os dados de um banco pelo Item")
    public ResponseEntity<InstitutionProfileResponse> findProfile(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID interno do Item") @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(bankService.findProfile(usuarioDetails.getId(), itemId));
    }

    @GetMapping("/{itemId}/transactions")
    @Operation(summary = "Lista as transacoes de um banco pelo Item")
    public ResponseEntity<Page<TransactionResponse>> findTransactions(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID interno do Item") @PathVariable UUID itemId,
            @RequestParam(defaultValue = "CHECKING") AccountType accountType,
            @ParameterObject @PageableDefault(
                    page = 0, size = 10, sort = "paymentDate", direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(bankService.findTransactions(
                usuarioDetails.getId(), itemId, accountType, pageable
        ));
    }
}
