package com.moneyMagnetApi.demo.dto.item.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateItemRequest(
        @NotNull(message = "O id do Item da Pluggy e obrigatorio")
        UUID pluggyItemId
) {
}
