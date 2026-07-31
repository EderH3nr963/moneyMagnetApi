package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceAuthorizationService {

    private final ItemRepository itemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final Cache<String, Item> itemByUserAndIdCache;

    public Item validateItem(UUID userId, UUID itemId) {
        return itemByUserAndIdCache.get(cacheKey(userId, itemId), key ->
                itemRepository.findByIdAndUsuarioId(itemId, userId)
                        .orElseThrow(() -> new AccessDeniedException("Item não encontrado."))
        );
    }

    public Account validateAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndItemUsuarioId(accountId, userId)
                .orElseThrow(() -> new AccessDeniedException("Conta não encontrada."));
    }

    public Transaction validateTransaction(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndAccountItemUsuarioId(transactionId, userId)
                .orElseThrow(() -> new AccessDeniedException("Transação não encontrada."));
    }

    public Category validateCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findAccessibleById(categoryId, userId)
                .orElseThrow(() -> new AccessDeniedException("Categoria não encontrada."));
    }

    private String cacheKey(UUID userId, UUID resourceId) {
        return userId + ":" + resourceId;
    }
}
