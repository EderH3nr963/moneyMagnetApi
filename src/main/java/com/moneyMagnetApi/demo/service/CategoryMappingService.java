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
        List<PluggyCategoryMapping> mappings = pluggyCategoryMappingRepository.findAll();
        
        return mappings.stream()
            .collect(Collectors.toMap(
                    mapping -> mapping.getPluggyCategory().getPluggyCategoryId(),
                    PluggyCategoryMapping::getCategory
            ));
    }
}
