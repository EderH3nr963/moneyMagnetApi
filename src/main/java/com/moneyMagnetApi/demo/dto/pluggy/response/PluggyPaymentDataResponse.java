package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyPaymentDataResponse(
        PluggyPaymentParticipantResponse payer,
        PluggyPaymentParticipantResponse receiver,
        String referenceNumber,
        String receiverReferenceId,
        String paymentMethod,
        String reason,
        PluggyBoletoMetadataResponse boletoMetadata
) {
}