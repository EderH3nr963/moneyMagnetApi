package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyAccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountSyncServiceTest {

    @InjectMocks
    private AccountSyncService accountSyncService;
    
    @Test
    public void resolveNumberShouldReturnNumberByNumberAccount() {
        String accountNumber = "accountNumber";
        
        PluggyAccountResponse pluggyAccountsResponse = PluggyAccountResponse.builder().number(accountNumber).build();
    }
}
