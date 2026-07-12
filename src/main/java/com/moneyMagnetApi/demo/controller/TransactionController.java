package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.TransactionService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transacoes", description = "Consulta, edicao e remocao de transacoes financeiras")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @GetMapping
    @Operation(
            summary = "Lista transacoes do usuario",
            description = "Retorna transacoes paginadas, com filtro opcional por periodo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagina de transacoes retornada"),
                    @ApiResponse(responseCode = "400", description = "Filtro de data invalido"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<Page<TransactionResponse>> findAll(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "Data inicial no formato yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Data final no formato yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "paymentDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        if (pageable.getPageSize() > 50)
            throw new BusinessException("Tamanho máximo de paginação é de 50", HttpStatus.BAD_REQUEST);
        
        return ResponseEntity.ok(
                transactionService.findAll(
                        usuarioDetails.getId(),
                        startDate,
                        endDate,
                        pageable
                )
        );
    }
    
    @PutMapping("/{transactionId}")
    @Operation(
            summary = "Atualiza dados editaveis de uma transacao",
            description = "Permite alterar descricao e/ou categoria de uma transacao.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transacao atualizada"),
                    @ApiResponse(responseCode = "404", description = "Transacao ou categoria nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da transacao") @PathVariable UUID transactionId,
            @Parameter(description = "Nova descricao da transacao")
            @RequestParam(required = false) String description,
            @Parameter(description = "ID da nova categoria")
            @RequestParam(required = false) UUID categoryId
    ) {
        
        UUID userId = usuarioDetails.getId();
        
        return ResponseEntity.ok(
                transactionService.update(
                        userId,
                        transactionId,
                        description,
                        categoryId
                )
        );
    }
    
}
