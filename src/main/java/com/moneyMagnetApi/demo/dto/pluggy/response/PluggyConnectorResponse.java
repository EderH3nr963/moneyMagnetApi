package com.moneyMagnetApi.demo.dto.pluggy.response;

public record PluggyConnectorResponse(
        Long id,
        String name,
        String imageUrl,
        String primaryColor
) {
}
