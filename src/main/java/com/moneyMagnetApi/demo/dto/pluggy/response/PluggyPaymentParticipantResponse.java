package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyPaymentParticipantResponse(
        String name,
        String branchNumber,
        String accountNumber,
        String routingNumber,
        String routingNumberISPB,
        PluggyDocumentNumberResponse documentNumber
) {
}