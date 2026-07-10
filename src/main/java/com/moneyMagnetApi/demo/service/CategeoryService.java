package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.category.request.CreateCategoryRequest;
import com.moneyMagnetApi.demo.dto.category.request.UpdateCategoryRequest;
import com.moneyMagnetApi.demo.dto.category.response.CategoryResponse;
import com.moneyMagnetApi.demo.dto.category.response.MerchantCategoryRuleResponse;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import com.moneyMagnetApi.demo.utils.StringNormalize;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategeoryService {
    
    private final CategoryRepository categoryRepository;
    private final UsuarioRepository usuarioRepository;
    private final Cache<UUID, List<CategoryResponse>> categoriesByUserCache;
    private final Cache<UUID, List<MerchantCategoryRuleResponse>> merchantCategoryRulesByUserCache;
    private final Cache<UUID, Map<String, Category>> activeMerchantCategoryRulesByUserCache;
    private final Cache<String, Map<String, Category>> pluggyCategoryMappingsCache;

    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String name = request.name().trim();
        String normalizedName = StringNormalize.normalize(name);

        if (categoryRepository.existsByNormalizedNameAndUsuarioId(normalizedName, userId)) {
            throw new ValidationException("Já existe uma categoria com esse nome");
        }

        Category category = Category.builder()
                .name(name)
                .normalizedName(normalizedName)
                .color(request.color())
                .icon(request.icon())
                .usuario(usuario)
                .build();

        CategoryResponse response = CategoryResponse.fromResponse(categoryRepository.save(category));
        invalidateCategoryRelatedCaches(userId);
        return response;
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(UUID userId) {
        return categoriesByUserCache.get(userId, this::loadCategories);
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndUsuarioId(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));

        String name = request.name().trim();
        String normalizedName = StringNormalize.normalize(name);

        if (categoryRepository.existsByNormalizedNameAndUsuarioIdAndIdNot(
                normalizedName,
                userId,
                categoryId
        )) {
            throw new ValidationException("Ja existe uma categoria com esse nome");
        }

        category.setName(name);
        category.setNormalizedName(normalizedName);
        category.setColor(request.color());
        category.setIcon(request.icon());

        CategoryResponse response = CategoryResponse.fromResponse(categoryRepository.save(category));
        invalidateCategoryRelatedCaches(userId);
        return response;
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndUsuarioId(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada"));

        categoryRepository.delete(category);
        invalidateCategoryRelatedCaches(userId);
    }

    private List<CategoryResponse> loadCategories(UUID userId) {
        List<Category> categories =
                categoryRepository.findAllByUsuarioIdOrUsuarioIsNullOrderByNameAsc(userId);

        return categories.stream().map(CategoryResponse::fromResponse).toList();
    }

    private void invalidateCategoryRelatedCaches(UUID userId) {
        categoriesByUserCache.invalidate(userId);
        merchantCategoryRulesByUserCache.invalidateAll();
        activeMerchantCategoryRulesByUserCache.invalidateAll();
        pluggyCategoryMappingsCache.invalidateAll();
    }
    
}
