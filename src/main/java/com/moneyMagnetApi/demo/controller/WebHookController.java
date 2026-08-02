package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.webhook.requests.ItemCreatedDTO;
import com.moneyMagnetApi.demo.dto.webhook.requests.ItemUpdatedDTO;
import com.moneyMagnetApi.demo.dto.webhook.requests.TransactionCreatedDTO;
import com.moneyMagnetApi.demo.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/pluggy")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Notificacoes recebidas da Pluggy")
public class WebHookController {

    private final ItemService itemService;

    @PostMapping("/item-updated")
    @Operation(summary = "Recebe o evento item/updated",
            responses = @ApiResponse(responseCode = "204", description = "Evento aceito"))
    public ResponseEntity<Void> itemUpdated(@Valid @RequestBody ItemUpdatedDTO dto) {
        itemService.itemUpdated(dto);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/item-created")
    @Operation(summary = "Recebe o evento item/created",
            responses = @ApiResponse(responseCode = "204", description = "Evento aceito"))
    public ResponseEntity<Void> itemCreated(@Valid @RequestBody ItemCreatedDTO dto) {
        itemService.itemCreated(dto);
        return ResponseEntity.noContent().build();
    }

//    @PostMapping("/transactions-created")
//    @Operation(summary = "Recebe o evento transactions/created",
//            responses = @ApiResponse(responseCode = "204", description = "Evento aceito"))
//    public ResponseEntity<Void> transactionsCreated(@Valid @RequestBody TransactionCreatedDTO dto) {
//        webHookService.transactionCreated(dto);
//        return ResponseEntity.noContent().build();
//    }
}
