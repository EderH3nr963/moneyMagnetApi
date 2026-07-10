package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.institution.response.InstitutionProfileResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.InstitutionRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private static final List<TransactionNature> DEFAULT_NATURES = List.of(
            TransactionNature.INCOME,
            TransactionNature.EXPENSE
    );

    private final InstitutionRepository institutionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public InstitutionProfileResponse findProfile(UUID userId, UUID institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new EntityNotFoundException("Instituicao nao encontrada."));
        List<Account> accounts =
                accountRepository.findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
                        institutionId,
                        userId
                );

        if (accounts.isEmpty()) {
            throw new EntityNotFoundException("Instituicao nao encontrada para este usuario.");
        }

        return new InstitutionProfileResponse(
                institution.getId(),
                institution.getName(),
                institution.getLogoUrl(),
                institution.getPrimaryColor(),
                accounts.stream().map(AccountResponse::fromAccount).toList()
        );
    }

    public Page<TransactionResponse> findTransactions(
            UUID userId,
            UUID institutionId,
            AccountType accountType,
            Pageable pageable
    ) {
        if (accountRepository
                .findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(institutionId, userId)
                .isEmpty()) {
            throw new EntityNotFoundException("Instituicao nao encontrada para este usuario.");
        }

        return transactionRepository.findAllByUserAndInstitutionAndAccountType(
                userId,
                institutionId,
                accountType,
                DEFAULT_NATURES,
                pageable
        ).map(TransactionResponse::fromResponse);
    }
}
