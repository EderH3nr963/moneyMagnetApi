package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.institution.Institution;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.institution.response.InstitutionProfileResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import com.moneyMagnetApi.demo.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Null;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private InstitutionRepository institutionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private InstitutionService institutionService;
    
    @Test
    void shouldReturnInstitutionProfileResponse() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        
        Usuario usuario = Usuario.builder()
                .id(userId)
                .build();
        
        Institution institution = Institution.builder()
                .id(institutionId)
                .name("Banco Teste")
                .logoUrl("logo.png")
                .primaryColor("#000000")
                .build();
        
        Item item = Item.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .institution(institution)
                .build();
        
        Account account = Account.builder()
                .id(accountId)
                .item(item)
                .build();
        
        Mockito.when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.of(institution));
        
        Mockito.when(
                accountRepository
                        .findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
                                institutionId,
                                userId
                        )
        ).thenReturn(List.of(account));
        
        InstitutionProfileResponse result =
                institutionService.findProfile(userId, institutionId);
        
        assertNotNull(result);
        assertEquals(institutionId, result.id());
        assertEquals("Banco Teste", result.name());
        assertEquals("logo.png", result.logoUrl());
        assertEquals("#000000", result.primaryColor());
        assertEquals(1, result.accounts().size());
        
        Mockito.verify(institutionRepository).findById(institutionId);
        
        Mockito.verify(accountRepository)
                .findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
                        institutionId,
                        userId
                );
    }
    
    @Test
    void shoudReturnEntityNotFoundExceptionWhenExecuteAccountRepository() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        
        Institution institution = Institution.builder()
                .id(institutionId)
                .build();
        
        Mockito.when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.of(institution));
        
        Mockito.when(
                accountRepository
                        .findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
                                institutionId,
                                userId
                        )
        ).thenReturn(List.of());
        
        assertThrows(
                EntityNotFoundException.class,
                () -> institutionService.findProfile(userId, institutionId)
        );
        
    }
    
    @Test
    void shoudReturnEntityNotFoundExceptionWhenExecuteInstitutionRepository() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        
        Mockito.when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.empty());
        
        assertThrows(
                EntityNotFoundException.class,
                () -> institutionService.findProfile(userId, institutionId)
        );
        
    }
    
    @Test
    void shouldReturnListOfTransactionResponse() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        
        Account account = Account.builder().id(UUID.randomUUID()).type(AccountType.CHECKING).build();
        Usuario user = Usuario.builder().id(userId).build();
        Institution institution = Institution.builder().id(institutionId).build();
        
        Item item =  Item.builder().id(UUID.randomUUID()).institution(institution).accounts(List.of(account)).usuario(user).build();
        
        List<Transaction> transactions = List.of(
                Transaction.builder().id(UUID.randomUUID()).type(TransactionType.CREDIT).account(account).build(),
                Transaction.builder().id(UUID.randomUUID()).type(TransactionType.DEBIT).account(account).build()
        );
        
        account.setTransactions(transactions);
        
        Page<Transaction> transactionPage = new PageImpl<>(
                transactions,
                pageable,
                transactions.size()
        );
        
        Mockito.when(
                accountRepository
                        .findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
                                institutionId,
                                userId
                        )
        ).thenReturn(List.of(account));
        
        Mockito.when(
                transactionRepository.findAllByUserAndInstitutionAndAccountType(
                    userId,
                        institutionId,
                        AccountType.CHECKING,
                        List.of(
                                TransactionNature.INCOME,
                                TransactionNature.EXPENSE
                        ),
                        pageable
                )
        ).thenReturn(transactionPage);
        
        Page<TransactionResponse> result = institutionService.findTransactions(userId, institutionId, AccountType.CHECKING, pageable);
        
        assertEquals(2, result.getContent().size());
        assertTrue(result.stream().map(transaction -> transaction.type()).toList().containsAll(List.of(TransactionType.CREDIT, TransactionType.DEBIT)));
    }
}