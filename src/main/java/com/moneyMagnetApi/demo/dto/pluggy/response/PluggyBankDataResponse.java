package com.moneyMagnetApi.demo.dto.pluggy.response;

public record PluggyBankDataResponse(
        
        String transferNumber,
        Long closingBalance,
        Long automaticallyInvestedBalance,
        Long overdraftContractedLimit,
        Long overdraftUsedLimit,
        Long unarrangedOverdraftAmount

) {
}