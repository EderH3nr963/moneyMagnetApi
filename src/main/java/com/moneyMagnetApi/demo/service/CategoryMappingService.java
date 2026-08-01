package com.moneyMagnetApi.demo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.category.PluggyCategory;
import com.moneyMagnetApi.demo.domain.category.PluggyCategoryMapping;
import com.moneyMagnetApi.demo.repository.PluggyCategoryMappingRepository;
import com.moneyMagnetApi.demo.repository.PluggyCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryMappingService {
    
    private final PluggyCategoryRepository pluggyCategoryRepository;
    private final PluggyCategoryMappingRepository pluggyCategoryMappingRepository;
    private final Cache<String, Map<String, Category>> pluggyCategoryMappingsCache;
    
    @Transactional(readOnly = true)
    public Map<String, Category> getCategories() {
        return pluggyCategoryMappingsCache.get("all", key -> loadCategories());
    }

    private Map<String, Category> loadCategories() {
        List<PluggyCategory> pluggyCategories = pluggyCategoryRepository.findAll();
        List<PluggyCategoryMapping> mappings = pluggyCategoryMappingRepository.findAll();
        
        Map<UUID, Category> mappedCategories = mappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getPluggyCategory().getId(),
                        PluggyCategoryMapping::getCategory
                ));
        
        Map<String, Category> result = new HashMap<>();
        
        for (PluggyCategory pluggyCategory : pluggyCategories) {
            
            Category category = mappedCategories.get(pluggyCategory.getId());
            
            if (category != null) {
                result.put(pluggyCategory.getPluggyCategoryId(), category);
            }
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public Category getCategory(String pluggyCategoryId) {
        if (!StringUtils.hasText(pluggyCategoryId)) {
            return null;
        }
        
        String normalizedPluggyCategoryId = pluggyCategoryId.trim();

        PluggyCategory pluggyCategory = pluggyCategoryRepository
                .findByPluggyCategoryId(normalizedPluggyCategoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Categoria da Pluggy nao encontrada: " + normalizedPluggyCategoryId
                        ));

        return findMappedCategory(pluggyCategory)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Categoria da aplicacao nao mapeada para: "
                                        + buildCategoryPath(pluggyCategory)
                        ));
    }

    private Optional<Category> findMappedCategory(PluggyCategory pluggyCategory) {
        PluggyCategory currentCategory = pluggyCategory;

        while (currentCategory != null) {
            Optional<Category> mappedCategory = pluggyCategoryMappingRepository
                    .findByPluggyCategory(currentCategory)
                    .map(PluggyCategoryMapping::getCategory);

            if (mappedCategory.isPresent()) {
                return mappedCategory;
            }

            currentCategory = currentCategory.getParent();
        }

        return Optional.empty();
    }

    private String buildCategoryPath(PluggyCategory pluggyCategory) {
        if (pluggyCategory.getParent() == null) {
            return pluggyCategory.getDescription();
        }

        return pluggyCategory.getParent().getDescription()
                + " > "
                + pluggyCategory.getDescription();
    }
}
