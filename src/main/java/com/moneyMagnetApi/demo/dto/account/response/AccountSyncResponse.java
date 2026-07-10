package com.moneyMagnetApi.demo.dto.account.response;

public record AccountSyncResponse(
        int received,
        int created,
        int updated,
        int skipped
) {
}