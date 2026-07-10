package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.item.request.CreateItemRequest;
import com.moneyMagnetApi.demo.dto.item.response.ItemSyncResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectorResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyItemResponse;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.repository.InstitutionRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private static final Set<String> SYNCHRONIZABLE_STATUSES =
            Set.of("SUCCESS", "PARTIAL_SUCCESS");

    private final PluggyClient pluggyClient;
    private final ItemRepository itemRepository;
    private final InstitutionRepository institutionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccountSyncService accountSyncService;
    private final TransactionSyncService transactionSyncService;
    private final AppCacheInvalidationService cacheInvalidationService;

    public ItemSyncResponse createAndSync(UUID usuarioId, CreateItemRequest request) {
        String pluggyItemId = request.pluggyItemId().toString();
        PluggyItemResponse pluggyItem = pluggyClient.getItem(pluggyItemId);

        validatePluggyItem(usuarioId, pluggyItemId, pluggyItem);

        Institution institution = upsertInstitution(pluggyItem.connector());
        Item item = upsertItem(usuarioId, pluggyItem, institution);

        List<Account> accounts = accountSyncService.syncItemNow(usuarioId, item.getId());
        int transactionsSynced = accounts.stream()
                .mapToInt(transactionSyncService::syncTransactionsNow)
                .sum();
        cacheInvalidationService.invalidateUserData(usuarioId);

        return new ItemSyncResponse(
                item.getId(),
                item.getPluggyItemId(),
                item.getStatus(),
                item.getExecutionStatus(),
                accounts.size(),
                transactionsSynced
        );
    }

    private void validatePluggyItem(
            UUID usuarioId,
            String requestedItemId,
            PluggyItemResponse pluggyItem
    ) {
        if (!requestedItemId.equals(pluggyItem.id())) {
            throw new BusinessException(
                    "O Item retornado pela Pluggy nao corresponde ao Item solicitado.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        if (!usuarioId.toString().equals(pluggyItem.clientUserId())) {
            throw new AccessDeniedException("O Item da Pluggy nao pertence ao usuario autenticado.");
        }

        if (!StringUtils.hasText(pluggyItem.executionStatus())
                || !SYNCHRONIZABLE_STATUSES.contains(pluggyItem.executionStatus())) {
            throw new BusinessException(
                    "O Item ainda nao terminou a sincronizacao na Pluggy. Status: "
                            + pluggyItem.executionStatus(),
                    HttpStatus.CONFLICT
            );
        }

        PluggyConnectorResponse connector = pluggyItem.connector();
        if (connector == null || connector.id() == null || !StringUtils.hasText(connector.name())) {
            throw new BusinessException(
                    "A Pluggy nao retornou os dados da instituicao do Item.",
                    HttpStatus.BAD_GATEWAY
            );
        }
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
