package com.moneyMagnetApi.demo.dto.pluggy.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyDocumentNumberResponse(
        String type,
        String value
) {
}