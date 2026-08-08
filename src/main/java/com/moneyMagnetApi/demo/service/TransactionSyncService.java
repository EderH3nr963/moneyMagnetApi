package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionResponse;
import com.moneyMagnetApi.demo.mappers.PluggyTransactionMapper;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionSyncService {

    private final ConcurrentHashMap<String, Boolean> syncingAccounts = new ConcurrentHashMap<>();

    private final PluggyClient pluggyClient;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryMappingService categoryMappingService;
    private final MerchantCategoryRuleService merchantCategoryRuleService;
    private final AppCacheInvalidationService cacheInvalidationService;
    private final @Qualifier("transactionSyncExecutor") Executor transactionSyncExecutor;

    @Transactional
    public int syncTransactionsNow(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Conta nao informada.");
        }

        if (syncingAccounts.putIfAbsent(account.getId().toString(), true) != null) {
            return 0;
        }

        try {
            if (!StringUtils.hasText(account.getPluggyAccountId())) {
                throw new IllegalArgumentException("Conta sem id da Pluggy.");
            }

            List<PluggyTransactionResponse> transactions = pluggyClient.getTransactions(account.getPluggyAccountId(),
                    account.getLastTransactionSync());

            List<Transaction> entities = new ArrayList<>();

            List<String> ids = transactions.stream()
                    .map(PluggyTransactionResponse::id)
                    .toList();
            Map<String, Transaction> existing = transactionRepository
                    .findAllByPluggyTransactionIdIn(ids)
                    .stream()
                    .collect(Collectors.toMap(
                            Transaction::getPluggyTransactionId,
                            Function.identity()));

            Map<String, Category> mapCategories = categoryMappingService.getCategories();
            
            UUID userId = account.getItem().getUsuario().getId();
            Map<String, Category> merchantRules = merchantCategoryRuleService.getActiveRulesByMerchant(userId);

            for (PluggyTransactionResponse dto : transactions) {
                if (!StringUtils.hasText(dto.id()) || existing.containsKey(dto.id())) {
                    continue;
                }

                Transaction transaction = new Transaction();

                Category category = mapCategories.get(dto.categoryId());
                if (category == null) {
                    category = mapCategories.get("99999999");
                }

                Category merchantCategory = merchantCategoryRuleService.resolveCategoryForMerchant(
                        merchantRules,
                        PluggyTransactionMapper.resolveMerchant(dto));
                if (merchantCategory != null) {
                    category = merchantCategory;
                }

                PluggyTransactionMapper.toEntity(transaction, account, category, dto);
                entities.add(transaction);
            }

            transactionRepository.saveAll(entities);

            account.setLastTransactionSync(LocalDateTime.now());
            accountRepository.save(account);
            cacheInvalidationService.invalidateTransactions();
            cacheInvalidationService.invalidateAccounts(userId);

            return entities.size();
        } finally {
            syncingAccounts.remove(account.getId().toString());
        }
    }
    
    public void syncTransactionsInParallel(List<Account> accounts) {
        CompletableFuture<?>[] synchronizations = accounts.stream()
                .map(account -> CompletableFuture.runAsync(
                        () -> syncTransactionsNow(account),
                        transactionSyncExecutor))
                .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(synchronizations).join();
    }
}
