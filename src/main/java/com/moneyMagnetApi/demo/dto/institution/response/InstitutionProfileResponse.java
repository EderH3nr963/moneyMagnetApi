package com.moneyMagnetApi.demo.dto.institution.response;

import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;

import java.util.List;
import java.util.UUID;

public record InstitutionProfileResponse(
        UUID id,
        String name,
        String logoUrl,
        String primaryColor,
        List<AccountResponse> accounts
) {
}
