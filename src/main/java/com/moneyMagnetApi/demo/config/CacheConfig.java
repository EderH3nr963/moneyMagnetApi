package com.moneyMagnetApi.demo.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.item.Item;
import com.moneyMagnetApi.demo.dto.account.response.AccountResponse;
import com.moneyMagnetApi.demo.dto.category.response.CategoryResponse;
import com.moneyMagnetApi.demo.dto.category.response.MerchantCategoryRuleResponse;
import com.moneyMagnetApi.demo.dto.transaction.response.TransactionResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<UUID, List<CategoryResponse>> categoriesByUserCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<UUID, List<MerchantCategoryRuleResponse>> merchantCategoryRulesByUserCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<UUID, Map<String, Category>> activeMerchantCategoryRulesByUserCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Map<String, Category>> pluggyCategoryMappingsCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterAccess(Duration.ofHours(2))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<UUID, List<AccountResponse>> accountsByUserCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, List<AccountResponse>> accountsByItemCache() {
        return Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, AccountResponse> accountByIdCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Page<TransactionResponse>> transactionsPageCache() {
        return Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, List<TransactionResponse>> transactionsByAccountCache() {
        return Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, TransactionResponse> transactionByIdCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Item> itemByUserAndIdCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterAccess(Duration.ofMinutes(15))
                .recordStats()
                .build();
    }
}
