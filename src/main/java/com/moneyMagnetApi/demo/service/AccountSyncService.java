package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountResponse;
import com.moneyMagnetApi.demo.mappers.AccountMapper;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountSyncService {
    
    private final ResourceAuthorizationService resourceAuthorizationService;
    private final PluggyClient pluggyClient;
    private final AccountRepository accountRepository;
    private final AppCacheInvalidationService cacheInvalidationService;
    
    @Transactional
    public List<Account> syncAccountsByItem(UUID userId, UUID itemId) {
        Item item = resourceAuthorizationService.validateItem(userId, itemId);
        
        List<PluggyAccountResponse> pluggyAccounts = pluggyClient.getAccounts(item.getPluggyItemId());
        LocalDateTime syncedAt = LocalDateTime.now();

        List<Account> entities = new ArrayList<>();
        Set<String> pluggyAccountIds = pluggyAccounts.stream()
                .map(PluggyAccountResponse::id)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        
        Map<String, Account> accountsByPluggyId =
                        accountRepository
                            .findAllByItemIdAndPluggyAccountIdIn(item.getId(), pluggyAccountIds)
                            .stream()
                            .collect(Collectors.toMap(Account::getPluggyAccountId, Function.identity()));

        
        for (PluggyAccountResponse response : pluggyAccounts) {
            if (!StringUtils.hasText(response.id())) {
                continue;
            }
            
            Account account = accountsByPluggyId.get(response.id());
            if (account == null) {
                account = new Account();
                account.setPluggyAccountId(response.id());
            }
            
            AccountMapper.toEntity(account, response, item, syncedAt);
            entities.add(account);
        }
        
        List<Account> savedAccounts = accountRepository.saveAll(entities);
        cacheInvalidationService.invalidateUserData(userId);

        return savedAccounts;
    }
   
}
