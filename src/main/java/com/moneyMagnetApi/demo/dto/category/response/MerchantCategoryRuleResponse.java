package com.moneyMagnetApi.demo.dto.category.response;

import com.moneyMagnetApi.demo.domain.category.MerchantCategoryRule;

import java.util.UUID;

public record MerchantCategoryRuleResponse(
        UUID id,
        String merchant,
        boolean active,
        CategoryResponse category
) {

    public static MerchantCategoryRuleResponse fromRule(MerchantCategoryRule rule) {
        return new MerchantCategoryRuleResponse(
                rule.getId(),
                rule.getMerchant(),
                rule.isActive(),
                CategoryResponse.fromResponse(rule.getCategory())
        );
    }
}
