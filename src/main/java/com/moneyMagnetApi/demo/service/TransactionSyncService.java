package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyTransactionResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    
    @Transactional
    @Async
    public void syncTransactions(Account account) {
        syncTransactionsInternal(account);
    }

    @Transactional
    public int syncTransactionsNow(Account account) {
        return syncTransactionsInternal(account);
    }

    private int syncTransactionsInternal(Account account) {
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
            
            List<PluggyTransactionResponse> transactions =
                    pluggyClient.getTransactions(account.getPluggyAccountId(), account.getLastTransactionSync());
            
            List<Transaction> entities = new ArrayList<>();
            
            List<String> ids = transactions.stream()
                    .map(PluggyTransactionResponse::id)
                    .toList();
            Map<String, Transaction> existing =
                    transactionRepository
                            .findAllByPluggyTransactionIdIn(ids)
                            .stream()
                            .collect(Collectors.toMap(
                                    Transaction::getPluggyTransactionId,
                                    Function.identity()
                            ));
            
            Map<String, Category> mapCategories = categoryMappingService.getCategories();
            UUID userId = account.getItem().getUsuario().getId();
            Map<String, Category> merchantRules =
                    merchantCategoryRuleService.getActiveRulesByMerchant(userId);
            
            for (PluggyTransactionResponse dto : transactions) {
                if (!StringUtils.hasText(dto.id())) {
                    continue;
                }

                Transaction transaction =
                        existing.getOrDefault(dto.id(), new Transaction());
                
                Category category = mapCategories.get(dto.categoryId());
                
                if (category == null) {
                    category = mapCategories.get("99999999");
                }

                Category merchantCategory =
                        merchantCategoryRuleService.resolveCategoryForMerchant(
                                merchantRules,
                                resolveMerchant(dto)
                        );
                if (merchantCategory != null) {
                    category = merchantCategory;
                }
                
                fillTransaction(transaction, account, category, dto);
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
    
    private void fillTransaction(
            Transaction transaction,
            Account account,
            Category category,
            PluggyTransactionResponse dto
    ) {
        
        transaction.setPluggyTransactionId(dto.id());
        transaction.setAccount(account);
        transaction.setDescription(resolveDescription(dto));
        transaction.setAmount(resolveAmount(dto));
        transaction.setCurrency(resolveCurrency(dto, account));
        transaction.setType(resolveType(dto));
        transaction.setStatus(resolveStatus(dto));
        
        LocalDateTime transactionDate = resolveDate(dto);
        transaction.setDate(transactionDate);
        transaction.setPaymentDate(transactionDate);
        transaction.setProviderCategory(dto.category());
        transaction.setProviderCode(dto.providerCode());
        transaction.setMerchant(resolveMerchant(dto));
        
        TransactionNature nature = resolveNature(dto, account);
        transaction.setNature(nature);
        
        transaction.setCategory(category);
    }
    
    private String resolveDescription(PluggyTransactionResponse dto) {
        if (StringUtils.hasText(dto.description())) {
            return dto.description().trim();
        }
        
        return "Transacao Pluggy";
    }
    
    TransactionNature resolveNature(
            PluggyTransactionResponse dto,
            Account account
    ) {
        if (StringUtils.hasText(dto.nature())) {
            String normalizedNature = dto.nature()
                    .trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
            try {
                TransactionNature receivedNature =
                        TransactionNature.valueOf(normalizedNature);
                if (receivedNature != TransactionNature.CREDIT_CARD_PAYMENT
                        || account.getType()
                        == com.moneyMagnetApi.demo.domain.account.AccountType.CREDIT) {
                    return receivedNature;
                }
            } catch (IllegalArgumentException ignored) {
                // Mantem compatibilidade quando a Pluggy enviar um valor novo.
            }
        }

        if ("05100000".equals(dto.categoryId())
                && account.getType()
                == com.moneyMagnetApi.demo.domain.account.AccountType.CREDIT) {
            return TransactionNature.CREDIT_CARD_PAYMENT;
        }
        if (dto.categoryId() != null && dto.categoryId().startsWith("040")) {
            return TransactionNature.INTERNAL_TRANSFER;
        }
        if (dto.categoryId() != null && dto.categoryId().startsWith("010")) {
            return TransactionNature.INCOME;
        }

        if (account.getType() == com.moneyMagnetApi.demo.domain.account.AccountType.CREDIT) {
            return dto.amount() != null && dto.amount().compareTo(BigDecimal.ZERO) < 0
                    ? TransactionNature.CREDIT_CARD_PAYMENT
                    : TransactionNature.EXPENSE;
        }

        return resolveType(dto) == TransactionType.CREDIT
                ? TransactionNature.INCOME
                : TransactionNature.EXPENSE;
    }
    
    private BigDecimal resolveAmount(PluggyTransactionResponse dto) {
        if (dto.amount() == null) {
            throw new IllegalArgumentException("Transacao sem valor: " + dto.id());
        }
        
        return dto.amount();
    }
    
    private String resolveCurrency(PluggyTransactionResponse dto, Account account) {
        if (StringUtils.hasText(dto.currencyCode())) {
            return dto.currencyCode().trim().toUpperCase();
        }
        if (StringUtils.hasText(account.getCurrency())) {
            return account.getCurrency();
        }
        
        throw new IllegalArgumentException("Transacao sem moeda configurada: " + dto.id());
    }
    
    private String resolveMerchant(PluggyTransactionResponse dto) {
        return dto.merchant() == null ? null : dto.merchant().name();
    }
    
    private TransactionStatus resolveStatus(PluggyTransactionResponse dto) {
        if (dto.status() == null) {
            return TransactionStatus.POSTED;
        }
        
        return dto.status();
    }
    
    private LocalDateTime resolveDate(PluggyTransactionResponse dto) {
        if (dto.date() == null) {
            throw new IllegalArgumentException("Transacao sem data: " + dto.id());
        }
        
        return dto.date().atZone(ZoneOffset.UTC).toLocalDateTime();
    }
    
    private TransactionType resolveType(PluggyTransactionResponse dto) {
        if (StringUtils.hasText(dto.type())) {
            try {
                return TransactionType.valueOf(dto.type().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return resolveTypeByAmount(dto);
            }
        }
        
        return resolveTypeByAmount(dto);
    }
    
    private TransactionType resolveTypeByAmount(PluggyTransactionResponse dto) {
        if (dto.amount() != null && dto.amount().compareTo(BigDecimal.ZERO) < 0) {
            return TransactionType.DEBIT;
        }
        
        return TransactionType.CREDIT;
    }
}
