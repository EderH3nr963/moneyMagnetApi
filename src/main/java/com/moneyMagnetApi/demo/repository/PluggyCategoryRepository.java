package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.category.PluggyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PluggyCategoryRepository extends JpaRepository<PluggyCategory, UUID> {
    Optional<PluggyCategory> findByPluggyCategoryId(String id);
}
