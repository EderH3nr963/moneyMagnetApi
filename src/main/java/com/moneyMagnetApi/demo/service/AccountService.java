package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuthorizationService authorizationService;
    private final AccountSyncService accountSyncService;
    private final TransactionSyncService transactionSyncService;
    private final Cache<UUID, List<AccountResponse>> accountsByUserCache;
    private final Cache<String, List<AccountResponse>> accountsByItemCache;
    private final Cache<String, AccountResponse> accountByIdCache;
    private final AppCacheInvalidationService cacheInvalidationService;

    public List<AccountResponse> findAll(UUID userId) {
        return accountsByUserCache.get(userId, this::loadAll);
    }

    private List<AccountResponse> loadAll(UUID userId) {
        List<Account> accounts = accountRepository.findAllByItemUsuarioIdOrderByNameAsc(userId);
        Set<UUID> staleItemIds = findStaleItemIds(accounts);

        staleItemIds.forEach(itemId -> accountSyncService.syncItem(userId, itemId));
        
        accounts.stream().filter(this::shouldSyncTransaction).forEach(transactionSyncService::syncTransactions);

        return accounts
                .stream()
                .map(AccountResponse::fromAccount)
                .toList();
    }

    public List<AccountResponse> findByItem(UUID userId, UUID itemId) {
        return accountsByItemCache.get(cacheKey(userId, itemId), key -> loadByItem(userId, itemId));
    }

    private List<AccountResponse> loadByItem(UUID userId, UUID itemId) {
        authorizationService.validateItem(userId, itemId);

        List<Account> accounts =
                accountRepository.findAllByItemIdAndItemUsuarioIdOrderByNameAsc(itemId, userId);

        if (accounts.stream().anyMatch(this::shouldSync)) {
            accountSyncService.syncItem(userId, itemId);
        }

        return accounts
                .stream()
                .map(AccountResponse::fromAccount)
                .toList();
    }

    private Set<UUID> findStaleItemIds(List<Account> accounts) {
        return accounts.stream()
                .filter(this::shouldSync)
                .map(account -> account.getItem().getId())
                .collect(Collectors.toSet());
    }

    private boolean shouldSync(Account account) {
        LocalDateTime lastSync = account.getLastAccountSync();
        return lastSync == null || lastSync.isBefore(LocalDateTime.now().minusHours(6));
    }
    
    private boolean shouldSyncTransaction(Account account) {
        LocalDateTime lastSync = account.getLastAccountSync();
        return lastSync == null || lastSync.isBefore(LocalDateTime.now().minusHours(6));
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID userId, UUID accountId) {
        return accountByIdCache.get(cacheKey(userId, accountId), key -> loadById(userId, accountId));
    }

    private AccountResponse loadById(UUID userId, UUID accountId) {
        Account account = authorizationService.validateAccount(userId, accountId);
        
        if (shouldSync(account)) accountSyncService.syncItem(userId, account.getItem().getId());
        
        return AccountResponse.fromAccount(
                account
        );
    }

    @Transactional
    public void delete(UUID userId, UUID accountId) {
        Account account = authorizationService.validateAccount(userId, accountId);
        accountRepository.delete(account);
        cacheInvalidationService.invalidateUserData(userId);
    }

    private String cacheKey(UUID userId, UUID resourceId) {
        return userId + ":" + resourceId;
    }
}
