package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.category.PluggyCategory;
import com.moneyMagnetApi.demo.domain.category.PluggyCategoryMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PluggyCategoryMappingRepository extends JpaRepository<PluggyCategoryMapping, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<PluggyCategoryMapping> findByPluggyCategory(
            PluggyCategory pluggyCategory
    );
}
