package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountSyncService {
    
    private final ConcurrentHashMap<String, Boolean> syncingItems = new ConcurrentHashMap<>();
    
    private final AuthorizationService authorizationService;
    private final PluggyClient pluggyClient;
    private final AccountRepository accountRepository;
    private final AppCacheInvalidationService cacheInvalidationService;
    
    @Transactional
    @Async
    public void syncItem(UUID userId, UUID itemId) {
        syncItemInternal(userId, itemId);
    }

    @Transactional
    public List<Account> syncItemNow(UUID userId, UUID itemId) {
        return syncItemInternal(userId, itemId);
    }

    private List<Account> syncItemInternal(UUID userId, UUID itemId) {
        Item item = authorizationService.validateItem(userId, itemId);

        if (syncingItems.putIfAbsent(itemId.toString(), true) != null) {
            return List.of();
        }

        try {
            List<PluggyAccountResponse> pluggyAccounts = pluggyClient.getAccounts(item.getPluggyItemId());
            LocalDateTime syncedAt = LocalDateTime.now();

            List<Account> entities = new ArrayList<>();
            Set<String> pluggyAccountIds = pluggyAccounts.stream()
                    .map(PluggyAccountResponse::id)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            
            Map<String, Account> accountsByPluggyId;
            Set<String> existingPluggyAccountIds;
            if (pluggyAccountIds.isEmpty()) {
                accountsByPluggyId = Map.of();
                existingPluggyAccountIds = Set.of();
            } else {
                accountsByPluggyId = accountRepository
                        .findAllByItemIdAndPluggyAccountIdIn(item.getId(), pluggyAccountIds)
                        .stream()
                        .collect(Collectors.toMap(Account::getPluggyAccountId, Function.identity()));
                existingPluggyAccountIds = accountRepository
                        .findExistingPluggyAccountIdsIncludingDeleted(pluggyAccountIds);
            }
            
            for (PluggyAccountResponse response : pluggyAccounts) {
                if (!StringUtils.hasText(response.id())) {
                    continue;
                }
                
                Account account = accountsByPluggyId.get(response.id());
                if (account == null) {
                    if (existingPluggyAccountIds.contains(response.id())) {
                        continue;
                    }
                    account = new Account();
                    account.setPluggyAccountId(response.id());
                }
                
                fillAccount(account, item, response, syncedAt);
                entities.add(account);
            }
            
            List<Account> savedAccounts = accountRepository.saveAll(entities);
            cacheInvalidationService.invalidateUserData(userId);

            return savedAccounts;
        } finally {
            syncingItems.remove(itemId.toString());
        }
    }
    
    private void fillAccount(
            Account account,
            Item item,
            PluggyAccountResponse response,
            LocalDateTime syncedAt
    ) {
        account.setItem(item);
        account.setName(resolveName(response));
        account.setType(resolveType(response));
        account.setSubtype(response.subtype());
        account.setCurrency(resolveCurrency(response.currencyCode()));
        account.setBalance(response.balance() == null ? BigDecimal.ZERO : response.balance());
        account.setCreditLimit(response.creditData() == null ? null : response.creditData().creditLimit());
        account.setNumber(resolveNumber(response));
        account.setLastAccountSync(syncedAt);
    }
    
    private String resolveName(PluggyAccountResponse response) {
        if (StringUtils.hasText(response.marketingName())) {
            return response.marketingName().trim();
        }
        if (StringUtils.hasText(response.name())) {
            return response.name().trim();
        }
        return "Conta";
    }
    
    private AccountType resolveType(PluggyAccountResponse response) {
        String value = ((response.type() == null ? "" : response.type()) + " "
                + (response.subtype() == null ? "" : response.subtype()))
                .toUpperCase(Locale.ROOT);
        
        if (value.contains("CHECKING") || value.contains("CONTA_CORRENTE")) return AccountType.CHECKING;
        if (value.contains("SAVING") || value.contains("POUPANCA")) return AccountType.SAVINGS;
        if (value.contains("CREDIT") || value.contains("CARTAO")) return AccountType.CREDIT;
        if (value.contains("LOAN") || value.contains("EMPRESTIMO")) return AccountType.LOAN;
        if (value.contains("INVEST")) return AccountType.INVESTMENT;
        if (value.contains("WALLET")) return AccountType.WALLET;
        if (value.contains("PREPAID")) return AccountType.PREPAID;
        return AccountType.OTHER;
    }
    
    private String resolveCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "BRL";
    }
    
    private String resolveNumber(PluggyAccountResponse response) {
        if (StringUtils.hasText(response.number())) {
            return response.number().trim();
        }
        if (response.bankData() != null && StringUtils.hasText(response.bankData().transferNumber())) {
            return response.bankData().transferNumber().trim();
        }
        return null;
    }
}
