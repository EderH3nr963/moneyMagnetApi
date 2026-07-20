package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final List<TransactionNature> DEFAULT_NATURES = List.of(
            TransactionNature.INCOME,
            TransactionNature.EXPENSE
    );
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthorizationService authorizationService;
    private final TransactionSyncService transactionSyncService;
    private final Cache<String, Page<TransactionResponse>> transactionsPageCache;
    private final Cache<String, List<TransactionResponse>> transactionsByAccountCache;
    private final Cache<String, TransactionResponse> transactionByIdCache;
    private final AppCacheInvalidationService cacheInvalidationService;
    
    private boolean shouldSync(Account account) {
        LocalDateTime lastSync = account.getLastTransactionSync();
        
        return lastSync == null
                || lastSync.isBefore(LocalDateTime.now().minusHours(6));
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            UUID accountId,
            Pageable pageable
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data inicial nao pode ser maior que a data final.");
        }

        List<Account> accounts =
                accountRepository.findAllByItemUsuarioIdOrderByNameAsc(userId);
        
        for (Account account : accounts) {
            if (shouldSync(account)) {
                transactionSyncService.syncTransactions(account);
            }
        }

        return transactionsPageCache.get(
                transactionPageCacheKey(userId, startDate, endDate, pageable),
                key -> loadTransactionsPage(userId, startDate, endDate, accountId, pageable)
        );
    }

    private Page<TransactionResponse> loadTransactionsPage(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            UUID accountId,
            Pageable pageable
    ) {
        
        LocalDateTime startDateTime = startDate != null
                ? startDate.atStartOfDay()
                : null;
        LocalDateTime endDateTime = endDate != null
                ? endDate.plusDays(1).atStartOfDay()
                : null;

        Page<Transaction> transactions = transactionRepository.findWithFilters(
                userId,
                DEFAULT_NATURES,
                accountId,
                startDate,
                endDate,
                pageable
        );
        
        return transactions.map(TransactionResponse::fromResponse);
    }

    
    @Transactional(readOnly = true)
    public List<TransactionResponse> findByAccount(UUID userId, UUID accountId) {
        return transactionsByAccountCache.get(cacheKey(userId, accountId), key -> loadByAccount(userId, accountId));
    }

    private List<TransactionResponse> loadByAccount(UUID userId, UUID accountId) {
        
        Account account = accountRepository
                .findByIdAndItemUsuarioId(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Conta não encontrada."));
        
        if (shouldSync(account))
            transactionSyncService.syncTransactions(account);
        
        List<Transaction> transactions =
                transactionRepository.findAllByAccountAndNatureIn(
                        account,
                        DEFAULT_NATURES
                );
        
        return transactions.stream().map((transaction) -> TransactionResponse.fromResponse(transaction)).toList();
    }
    
    @Transactional
    public TransactionResponse update(
            UUID userId,
            UUID transactionId,
            String description,
            UUID categoryId
    ) {
        
        Transaction transaction = authorizationService.validateTransaction(userId, transactionId);
        
        if (StringUtils.hasText(description)) {
            transaction.setDescription(description.trim());
        }
        
        if (categoryId != null) {
            Category category = authorizationService.validateCategory(userId, categoryId);
            
            transaction.setCategory(category);
        }
        
        transactionRepository.save(transaction);
        cacheInvalidationService.invalidateTransactions();
        
        return TransactionResponse.fromResponse(transaction);
    }
    
    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID userId, UUID transactionId) {
        return transactionByIdCache.get(cacheKey(userId, transactionId), key -> {
            Transaction transaction = authorizationService.validateTransaction(userId, transactionId);

            return TransactionResponse.fromResponse(transaction);
        });
    }
    
    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        
        Transaction transaction = authorizationService.validateTransaction(userId, transactionId);
        
        transactionRepository.delete(transaction);
        cacheInvalidationService.invalidateTransactions();
    }

    private String cacheKey(UUID userId, UUID resourceId) {
        return userId + ":" + resourceId;
    }

    private String transactionPageCacheKey(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return userId
                + ":" + (startDate == null ? "" : startDate)
                + ":" + (endDate == null ? "" : endDate)
                + ":" + pageable.getPageNumber()
                + ":" + pageable.getPageSize()
                + ":" + pageable.getSort();
    }
    
}
