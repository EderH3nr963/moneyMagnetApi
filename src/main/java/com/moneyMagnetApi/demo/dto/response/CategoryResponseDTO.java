package com.moneyMagnetApi.demo.dto.response;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.category.CategoryType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

public record CategoryResponseDTO (
        UUID id,
        String name,
        @Enumerated(EnumType.STRING) CategoryType type,
        String color
) {
    public static CategoryResponseDTO fromCategory(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor()
        );
    }
}
