package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppCacheInvalidationService {

    private final Cache<UUID, List<AccountResponse>> accountsByUserCache;
    private final Cache<String, List<AccountResponse>> accountsByItemCache;
    private final Cache<String, AccountResponse> accountByIdCache;
    private final Cache<String, Page<TransactionResponse>> transactionsPageCache;
    private final Cache<String, List<TransactionResponse>> transactionsByAccountCache;
    private final Cache<String, TransactionResponse> transactionByIdCache;
    private final Cache<String, Item> itemByUserAndIdCache;

    public void invalidateAccounts(UUID userId) {
        accountsByUserCache.invalidate(userId);
        accountsByItemCache.invalidateAll();
        accountByIdCache.invalidateAll();
    }

    public void invalidateTransactions() {
        transactionsPageCache.invalidateAll();
        transactionsByAccountCache.invalidateAll();
        transactionByIdCache.invalidateAll();
    }

    public void invalidateUserData(UUID userId) {
        invalidateAccounts(userId);
        invalidateTransactions();
        itemByUserAndIdCache.invalidateAll();
    }
}
