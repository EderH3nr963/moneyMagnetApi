package com.moneyMagnetApi.demo.dto.webhook.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ItemCreatedDTO(
        @NotBlank
        @Pattern(regexp = "item/created")
        String event,
        @NotBlank String eventId,
        @NotBlank String itemId,
        @NotBlank String triggeredBy,
        String clientUserId
) {
}
