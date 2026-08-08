package com.moneyMagnetApi.demo.mappers;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.category.request.CreateCategoryRequest;
import com.moneyMagnetApi.demo.utils.StringNormalize;

public class CategoryMapper {
    public static Category toEntity(CreateCategoryRequest createCategoryRequest, Usuario usuario) {
        String name = createCategoryRequest.name().trim();
        String normalizedName = StringNormalize.normalize(name);
        
        return Category.builder()
                .name(name)
                .normalizedName(normalizedName)
                .color(createCategoryRequest.color())
                .icon(createCategoryRequest.icon())
                .usuario(usuario)
                .build();
    }
}
