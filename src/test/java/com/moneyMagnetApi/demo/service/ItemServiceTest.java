package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.item.request.CreateItemRequest;
import com.moneyMagnetApi.demo.dto.item.response.ItemSyncResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectorResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyItemResponse;
import com.moneyMagnetApi.demo.repository.InstitutionRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AccountSyncService accountSyncService;
    @Mock
    private TransactionSyncService transactionSyncService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void shouldCreateItemThenSyncAccountsAndTransactionsInOrder() {
        UUID usuarioId = UUID.fromString("3de57ed9-6174-47f9-bb54-c07fd0d1b3c2");
        UUID pluggyItemId = UUID.fromString("21d4c225-c4ff-42d2-89bf-f0623994c363");
        UUID localItemId = UUID.fromString("7283da95-31be-43cd-b7c0-2e8981116d0d");

        PluggyConnectorResponse connector = new PluggyConnectorResponse(
                201L,
                "Banco Teste",
                "https://cdn.pluggy.ai/banco.svg",
                "#123456"
        );
        PluggyItemResponse pluggyItem = new PluggyItemResponse(
                pluggyItemId.toString(),
                connector,
                "UPDATED",
                "SUCCESS",
                usuarioId.toString()
        );
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(pluggyClient.getItem(pluggyItemId.toString())).thenReturn(pluggyItem);
        when(institutionRepository.findByPluggyConnectorId("201")).thenReturn(Optional.empty());
        when(institutionRepository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByPluggyItemId(pluggyItemId.toString()))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(localItemId);
            return item;
        });

        Account firstAccount = new Account();
        firstAccount.setId(UUID.randomUUID());
        Account secondAccount = new Account();
        secondAccount.setId(UUID.randomUUID());
        when(accountSyncService.syncItemNow(usuarioId, localItemId))
                .thenReturn(List.of(firstAccount, secondAccount));
        when(transactionSyncService.syncTransactionsNow(firstAccount)).thenReturn(3);
        when(transactionSyncService.syncTransactionsNow(secondAccount)).thenReturn(2);

        ItemSyncResponse response = itemService.createAndSync(
                usuarioId,
                new CreateItemRequest(pluggyItemId)
        );

        assertThat(response.itemId()).isEqualTo(localItemId);
        assertThat(response.accountsSynced()).isEqualTo(2);
        assertThat(response.transactionsSynced()).isEqualTo(5);

        InOrder synchronizationOrder = inOrder(
                accountSyncService,
                transactionSyncService
        );
        synchronizationOrder.verify(accountSyncService)
                .syncItemNow(usuarioId, localItemId);
        synchronizationOrder.verify(transactionSyncService)
                .syncTransactionsNow(firstAccount);
        synchronizationOrder.verify(transactionSyncService)
                .syncTransactionsNow(secondAccount);
    }

    @Test
    void shouldRejectItemOwnedByAnotherUser() {
        UUID usuarioId = UUID.randomUUID();
        UUID pluggyItemId = UUID.randomUUID();
        PluggyItemResponse pluggyItem = new PluggyItemResponse(
                pluggyItemId.toString(),
                new PluggyConnectorResponse(201L, "Banco Teste", null, null),
                "UPDATED",
                "SUCCESS",
                UUID.randomUUID().toString()
        );

        when(pluggyClient.getItem(pluggyItemId.toString())).thenReturn(pluggyItem);

        assertThatThrownBy(() -> itemService.createAndSync(
                usuarioId,
                new CreateItemRequest(pluggyItemId)
        )).isInstanceOf(AccessDeniedException.class);

        verify(itemRepository, never()).save(any());
        verify(accountSyncService, never()).syncItemNow(any(), any());
    }
}
