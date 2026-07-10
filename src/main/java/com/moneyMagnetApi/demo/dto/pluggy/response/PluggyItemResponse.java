package com.moneyMagnetApi.demo.dto.pluggy.response;

public record PluggyItemResponse(
        String id,
        PluggyConnectorResponse connector,
        String status,
        String executionStatus,
        String clientUserId
) {
}
