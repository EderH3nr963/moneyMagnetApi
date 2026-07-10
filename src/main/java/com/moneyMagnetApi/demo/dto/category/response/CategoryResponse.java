package com.moneyMagnetApi.demo.dto.category.response;

import com.moneyMagnetApi.demo.domain.category.Category;

import java.util.UUID;

public record CategoryResponse(
        
        UUID id,
        
        String name,
        
        String icon,
        
        String color,

        boolean systemCategory
        
) {
    public static CategoryResponse fromResponse(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.isSystemCategory()
        );
    }
}
