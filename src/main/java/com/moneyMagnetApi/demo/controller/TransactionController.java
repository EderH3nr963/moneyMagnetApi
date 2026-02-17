package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.request.*;
import com.moneyMagnetApi.demo.dto.response.CategoryResponseDTO;
import com.moneyMagnetApi.demo.dto.response.PageTransactionResponseDTO;
import com.moneyMagnetApi.demo.dto.response.TransactionImportResponseDTO;
import com.moneyMagnetApi.demo.dto.response.TransactionResponseDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transaction")
@Tag(
        name = "Transacao",
        description = "Rotas de manipulação de tranasações para usuário"
)
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService =  transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponseDTO> create(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid CreateTransactionDTO dto
    ) {
        TransactionResponseDTO response = transactionService.create(usuarioDetails.getId(), dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value="/import/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionImportResponseDTO> importXlsx(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam("file") MultipartFile file
    ) {
        TransactionImportResponseDTO response = transactionService.importXlsx(usuarioDetails.getId(), file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getById(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PathVariable UUID transactionId
    ) {
        TransactionResponseDTO response = transactionService.getById(usuarioDetails.getId(), transactionId);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PageTransactionResponseDTO> getAllPageSortingByDate(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageTransactionResponseDTO response = transactionService.getAll(usuarioDetails.getId(), pageable);

        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID transactionId,
            @RequestBody UpdateTransactionDTO dto
    ) {
        UUID usuarioId = userDetails.getId();
        TransactionResponseDTO transaction = transactionService.update(usuarioId, transactionId, dto);
        return ResponseEntity.ok(transaction);
    }

    @PatchMapping("/{transactionId}/description")
    public ResponseEntity<TransactionResponseDTO> updateDescription(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PathVariable UUID transactionId,
            @RequestBody @Valid UpdateDescriptionTransactionDTO dto
    ) {
        TransactionResponseDTO response = transactionService.updateDescription(usuarioDetails.getId(), transactionId, dto);

        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/{transactionId}/amount")
    public ResponseEntity<TransactionResponseDTO> updateAmount(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PathVariable UUID transactionId,
            @RequestBody @Valid UpdateAmountTransactionDTO dto
    ) {
        TransactionResponseDTO response = transactionService.updateAmount(usuarioDetails.getId(), transactionId, dto);

        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/{transactionId}/date")
    public ResponseEntity<TransactionResponseDTO> updateDate(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PathVariable UUID transactionId,
            @RequestBody @Valid UpdateDateTransactionDTO dto
    ) {
        TransactionResponseDTO response = transactionService.updateDate(usuarioDetails.getId(), transactionId, dto);

        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @PathVariable UUID transactionId
    ) {
        transactionService.delete(usuarioDetails.getId(), transactionId);

        return ResponseEntity.noContent().build();
    }
}
