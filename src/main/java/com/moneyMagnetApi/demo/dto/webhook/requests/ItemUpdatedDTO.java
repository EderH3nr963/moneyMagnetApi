package com.moneyMagnetApi.demo.dto.webhook.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ItemUpdatedDTO(
    @NotBlank
    @Pattern(regexp = "item/updated")
    String event,
    @NotBlank String eventId,
    @NotBlank String itemId,
    @NotBlank String triggeredBy,
    String clientUserId
) {
}
