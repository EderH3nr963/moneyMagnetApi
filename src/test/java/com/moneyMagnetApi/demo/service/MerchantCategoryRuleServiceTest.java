package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.Category;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantCategoryRuleServiceTest {

    private final MerchantCategoryRuleService service =
            new MerchantCategoryRuleService(null, null, null, null, null, null);

    @Test
    void shouldResolveMerchantIgnoringCaseAndExtraSpaces() {
        Category category = new Category();

        Category resolvedCategory = service.resolveCategoryForMerchant(
                Map.of("uber", category),
                " UBER "
        );

        assertThat(resolvedCategory).isSameAs(category);
    }
}
