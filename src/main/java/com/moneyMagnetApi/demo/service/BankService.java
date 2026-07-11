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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankService {

    private static final List<TransactionNature> DEFAULT_NATURES = List.of(
            TransactionNature.INCOME,
            TransactionNature.EXPENSE
    );

    private final ItemRepository itemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<InstitutionProfileResponse> findAll(UUID userId) {
        return itemRepository.findAllByUsuarioId(userId).stream()
                .map(item -> toResponse(item, accountsFor(userId, item.getId())))
                .sorted(Comparator.comparing(InstitutionProfileResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public InstitutionProfileResponse findProfile(UUID userId, UUID itemId) {
        Item item = itemRepository.findByIdAndUsuarioId(itemId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Banco nao encontrado para este usuario."));

        return toResponse(item, accountsFor(userId, itemId));
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

    private List<Account> accountsFor(UUID userId, UUID itemId) {
        return accountRepository.findAllByItemIdAndItemUsuarioIdOrderByNameAsc(itemId, userId);
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
