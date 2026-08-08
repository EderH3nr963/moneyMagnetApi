package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.institution.response.InstitutionProfileResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankService {

    private static final List<TransactionNature> DEFAULT_NATURES = List.of(
            TransactionNature.INCOME,
            TransactionNature.EXPENSE,
            TransactionNature.REFUND
    );

    private final ItemRepository itemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ResourceAuthorizationService resourceAuthorizationService;
    
    public List<InstitutionProfileResponse> findAll(UUID userId) {
        List<Account> accounts =
                accountRepository.findAllByItemUsuarioIdOrderByNameAsc(userId);
        
        List<Item> items =
                itemRepository.findAllByUsuarioId(userId);
        
        Map<UUID, List<Account>> accountsByItem =
                accounts.stream()
                        .collect(Collectors.groupingBy(
                                account -> account.getItem().getId()
                        ));
        
        return items.stream()
                .map(item -> toResponse(
                        item,
                        accountsByItem.getOrDefault(item.getId(), List.of())
                ))
                .toList();
    }

    public InstitutionProfileResponse findProfile(UUID userId, UUID itemId) {
        Item item = resourceAuthorizationService.validateItem(userId, itemId);
        
        List<Account> accounts = accountRepository.findAllByItemIdAndItemUsuarioIdOrderByNameAsc(itemId, userId);

        return toResponse(item, accounts);
    }

    public Page<TransactionResponse> findTransactions(
            UUID userId,
            UUID itemId,
            AccountType accountType,
            Pageable pageable
    ) {
        if (itemRepository.findByIdAndUsuarioId(itemId, userId).isEmpty()) {
            throw new EntityNotFoundException("Banco nao encontrado para este usuario.");
        }

        return transactionRepository.findAllByUserAndItemAndAccountType(
                userId, itemId, accountType, DEFAULT_NATURES, pageable
        ).map(TransactionResponse::fromResponse);
    }

    private InstitutionProfileResponse toResponse(Item item, List<Account> accounts) {
        return new InstitutionProfileResponse(
                item.getId(),
                item.getInstitution().getName(),
                item.getInstitution().getLogoUrl(),
                item.getInstitution().getPrimaryColor(),
                accounts.stream().map(AccountResponse::fromAccount).toList()
        );
    }
}
