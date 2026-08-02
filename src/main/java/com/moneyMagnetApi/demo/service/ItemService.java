package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectorResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyItemResponse;
import com.moneyMagnetApi.demo.dto.webhook.requests.ItemCreatedDTO;
import com.moneyMagnetApi.demo.dto.webhook.requests.ItemUpdatedDTO;
import com.moneyMagnetApi.demo.repository.InstitutionRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {

    public static final String ITEM_CREATED_UPDATED_EVENT = "ITEM_CREATED_UPDATED";

    private static final Set<String> SYNCHRONIZABLE_STATUSES =
            Set.of("SUCCESS", "PARTIAL_SUCCESS");

    private final PluggyClient pluggyClient;
    private final ItemRepository itemRepository;
    private final InstitutionRepository institutionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccountSyncService accountSyncService;
    private final TransactionSyncService transactionSyncService;
    private final AppCacheInvalidationService cacheInvalidationService;
    private final DashboardSseService dashboardSseService;
    private final @Qualifier("transactionSyncExecutor") Executor transactionSyncExecutor;

    @Async("webhookTaskExecutor")
    public void itemCreated(ItemCreatedDTO dto) {
        String pluggyItemId = dto.itemId();
        UUID usuarioId = UUID.fromString(dto.clientUserId());
        
        PluggyItemResponse pluggyItem = pluggyClient.getItem(pluggyItemId);

        Institution institution = upsertInstitution(pluggyItem.connector());
        Item item = upsertItem(usuarioId, pluggyItem, institution);

        List<Account> accounts = accountSyncService.syncAccountsByItem(usuarioId, item.getId());
        syncTransactionsInParallel(accounts);
        
        cacheInvalidationService.invalidateUserData(usuarioId);
        
        dashboardSseService.emitToUser(usuarioId, ITEM_CREATED_UPDATED_EVENT,
                Map.of("status", "synchronized"));
    }
    
    @Async("webhookTaskExecutor")
    public void itemUpdated(ItemUpdatedDTO dto) {
        List<Account> accounts = accountSyncService.syncAccountsByItem(UUID.fromString(dto.clientUserId()), UUID.fromString(dto.itemId()));
        
        syncTransactionsInParallel(accounts);
        cacheInvalidationService.invalidateUserData(UUID.fromString(dto.clientUserId()));
        
        dashboardSseService.emitToUser(UUID.fromString(dto.clientUserId()), ITEM_CREATED_UPDATED_EVENT,
                Map.of("status", "synchronized"));
    }

    void syncTransactionsInParallel(List<Account> accounts) {
        CompletableFuture<?>[] synchronizations = accounts.stream()
                .map(account -> CompletableFuture.runAsync(
                        () -> transactionSyncService.syncTransactionsNow(account),
                        transactionSyncExecutor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(synchronizations).join();
    }

    private Institution upsertInstitution(PluggyConnectorResponse connector) {
        String connectorId = connector.id().toString();
        Institution institution = institutionRepository
                .findByPluggyConnectorId(connectorId)
                .orElseGet(Institution::new);

        institution.setPluggyConnectorId(connectorId);
        institution.setName(connector.name());
        institution.setLogoUrl(connector.imageUrl());
        institution.setPrimaryColor(connector.primaryColor());

        return institutionRepository.save(institution);
    }

    private Item upsertItem(
            UUID usuarioId,
            PluggyItemResponse pluggyItem,
            Institution institution
    ) {
        Item item = itemRepository.findByPluggyItemId(pluggyItem.id())
                .orElseGet(Item::new);

        if (item.getId() != null && !item.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("O Item ja esta vinculado a outro usuario.");
        }

        if (item.getUsuario() == null) {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
            item.setUsuario(usuario);
        }

        item.setPluggyItemId(pluggyItem.id());
        item.setInstitution(institution);
        item.setStatus(pluggyItem.status());
        item.setExecutionStatus(pluggyItem.executionStatus());

        return itemRepository.save(item);
    }
}
