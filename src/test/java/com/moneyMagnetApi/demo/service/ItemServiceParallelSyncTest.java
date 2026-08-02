package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.repository.InstitutionRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ItemServiceParallelSyncTest {

    @Test
    void shouldSynchronizeAccountsConcurrentlyAndWaitForAllOfThem() {
        TransactionSyncService transactionSyncService = mock(TransactionSyncService.class);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothStarted = new CountDownLatch(2);

        doAnswer(invocation -> {
            bothStarted.countDown();
            if (!bothStarted.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("As contas nao foram sincronizadas em paralelo.");
            }
            return 1;
        }).when(transactionSyncService).syncTransactionsNow(org.mockito.ArgumentMatchers.any());

        ItemService service = new ItemService(
                mock(PluggyClient.class),
                mock(ItemRepository.class),
                mock(InstitutionRepository.class),
                mock(UsuarioRepository.class),
                mock(AccountSyncService.class),
                transactionSyncService,
                mock(AppCacheInvalidationService.class),
                mock(DashboardSseService.class),
                executor
        );

        try {
            assertThatCode(() -> service.syncTransactionsInParallel(
                    List.of(new Account(), new Account())))
                    .doesNotThrowAnyException();
        } finally {
            executor.shutdownNow();
        }
    }
}
