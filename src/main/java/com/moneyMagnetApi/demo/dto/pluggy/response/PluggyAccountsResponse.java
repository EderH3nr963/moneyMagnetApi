package com.moneyMagnetApi.demo.dto.pluggy.response;

import java.util.List;

public record PluggyAccountsResponse(
        Integer page,
        Integer total,
        Integer totalPages,
        List<PluggyAccountResponse> results
) {
}