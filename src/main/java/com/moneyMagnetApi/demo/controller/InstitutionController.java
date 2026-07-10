package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.dto.institution.response.InstitutionProfileResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Instituicoes", description = "Perfil de instituicoes financeiras e suas transacoes")
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/{institutionId}")
    @Operation(
            summary = "Busca o perfil de uma instituicao",
            description = "Retorna dados da instituicao e contas vinculadas do usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Instituicao encontrada"),
                    @ApiResponse(responseCode = "404", description = "Instituicao nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<InstitutionProfileResponse> findProfile(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da instituicao") @PathVariable UUID institutionId
    ) {
        return ResponseEntity.ok(
                institutionService.findProfile(usuarioDetails.getId(), institutionId)
        );
    }

    @GetMapping("/{institutionId}/transactions")
    @Operation(
            summary = "Lista transacoes de uma instituicao",
            description = "Retorna transacoes paginadas filtradas pelo tipo de conta da instituicao.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagina de transacoes retornada"),
                    @ApiResponse(responseCode = "404", description = "Instituicao nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<Page<TransactionResponse>> findTransactions(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da instituicao") @PathVariable UUID institutionId,
            @Parameter(description = "Tipo de conta usado como filtro")
            @RequestParam(defaultValue = "CHECKING") AccountType accountType,
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "paymentDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                institutionService.findTransactions(
                        usuarioDetails.getId(),
                        institutionId,
                        accountType,
                        pageable
                )
        );
    }
}
