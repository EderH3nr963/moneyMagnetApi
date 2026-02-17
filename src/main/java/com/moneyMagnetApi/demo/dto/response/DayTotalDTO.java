package com.moneyMagnetApi.demo.dto.response;

import java.math.BigDecimal;

public record DayTotalDTO(
        Integer dia,
        BigDecimal total
) {}
