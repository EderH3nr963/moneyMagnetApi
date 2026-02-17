package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.category.CategoryType;

import java.math.BigDecimal;

public record CategoryTotalDTO(
        String name,
        BigDecimal total,
        CategoryType type
) {}
