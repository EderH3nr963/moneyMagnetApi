package com.moneyMagnetApi.demo.dto.item.response;

import java.util.UUID;

public record ItemSyncResponse(
        UUID itemId,
        String pluggyItemId,
        String status,
        String executionStatus,
        int accountsSynced,
        int transactionsSynced
) {
}
