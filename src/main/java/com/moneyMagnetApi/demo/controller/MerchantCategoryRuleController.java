package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.category.request.CreateMerchantCategoryRuleRequest;
import com.moneyMagnetApi.demo.dto.category.request.UpdateMerchantCategoryRuleRequest;
import com.moneyMagnetApi.demo.dto.category.response.MerchantCategoryRuleResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.MerchantCategoryRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories/merchant-rules")
@RequiredArgsConstructor
@Tag(
        name = "Regras por merchant",
        description = "Regras que categorizam automaticamente transacoes pelo merchant"
)
public class MerchantCategoryRuleController {

    private final MerchantCategoryRuleService merchantCategoryRuleService;

    @PostMapping
    @Operation(
            summary = "Cria uma regra de categoria por merchant",
            description = "Define que transacoes futuras de um merchant usem uma categoria especifica.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Regra criada"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<MerchantCategoryRuleResponse> create(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Valid @RequestBody CreateMerchantCategoryRuleRequest request
    ) {
        MerchantCategoryRuleResponse rule = merchantCategoryRuleService.create(usuarioDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping
    @Operation(
            summary = "Lista regras de merchant do usuario",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regras retornadas"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<List<MerchantCategoryRuleResponse>> findAll(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        return ResponseEntity.ok(merchantCategoryRuleService.findAll(usuarioDetails.getId()));
    }

    @PutMapping("/{ruleId}")
    @Operation(
            summary = "Atualiza uma regra de merchant",
            description = "Permite trocar a categoria vinculada e ativar ou desativar a regra.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Regra atualizada"),
                    @ApiResponse(responseCode = "404", description = "Regra nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<MerchantCategoryRuleResponse> update(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da regra") @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateMerchantCategoryRuleRequest request
    ) {
        return ResponseEntity.ok(
                merchantCategoryRuleService.update(usuarioDetails.getId(), ruleId, request)
        );
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove uma regra de merchant",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Regra removida"),
                    @ApiResponse(responseCode = "404", description = "Regra nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public void delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da regra") @PathVariable UUID ruleId
    ) {
        merchantCategoryRuleService.delete(usuarioDetails.getId(), ruleId);
    }
}
