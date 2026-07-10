package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.category.MerchantCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantCategoryRuleRepository extends JpaRepository<MerchantCategoryRule, UUID> {

    Optional<MerchantCategoryRule> findByIdAndUsuarioId(UUID id, UUID userId);

    Optional<MerchantCategoryRule> findByUsuarioIdAndNormalizedMerchant(UUID userId, String normalizedMerchant);

    boolean existsByUsuarioIdAndNormalizedMerchant(UUID userId, String normalizedMerchant);

    List<MerchantCategoryRule> findAllByUsuarioIdOrderByMerchantAsc(UUID userId);

    List<MerchantCategoryRule> findAllByUsuarioIdAndActiveTrue(UUID userId);
}
