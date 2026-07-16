package com.moneyMagnetApi.demo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.webhook.requests.ItemUpdatedDTO;
import com.moneyMagnetApi.demo.dto.webhook.requests.TransactionCreatedDTO;
import com.moneyMagnetApi.demo.repository.AccountRepository;
import com.moneyMagnetApi.demo.repository.ItemRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebHookService {
  
  private final AccountSyncService accountSyncService;
  private final TransactionSyncService transactionSyncService;
  private final AccountRepository accountRepository;
  private final ItemRepository itemRepository;

  public void itemUpdated(ItemUpdatedDTO dto) {
    Item item = itemRepository.findByPluggyItemId(dto.itemId()).orElseThrow(
      () -> new EntityNotFoundException("Item não foi achado")
    );
    List<Account> accounts = accountRepository.findAllByItemId(item.getId());

    accountSyncService.syncItem(UUID.fromString(dto.clientUserId()), item.getId());

    for (Account account: accounts) {
      transactionSyncService.syncTransactions(account);
    }
  }

  public void transactionCreated(TransactionCreatedDTO dto) {
    Account account = accountRepository.findByPluggyAccountId(dto.accountId()).orElseThrow(
      () -> new EntityNotFoundException("Conta nao encontrada")
    );

    transactionSyncService.syncTransactions(account);
  }

}
