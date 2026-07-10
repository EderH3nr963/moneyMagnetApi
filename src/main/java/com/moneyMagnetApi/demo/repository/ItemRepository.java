package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    Optional<Item> findByIdAndUsuarioId(UUID accountId, UUID userId);

    Optional<Item> findByPluggyItemId(String pluggyItemId);
    
    List<Item> findAllByUsuarioId(UUID userId);
}
