package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByIdAndUsuarioId(UUID id, UUID userId);
    List<Category> findAllByUsuarioId(UUID userId);

    boolean existsByNormalizedNameAndUsuarioId(String normalizedName, UUID usuarioId);
}
