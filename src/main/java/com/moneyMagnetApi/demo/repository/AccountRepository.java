package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.account.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @EntityGraph(attributePaths = {"item", "item.institution"})
    Optional<Account> findByIdAndItemUsuarioId(UUID accountId, UUID userId);

    List<Account> findAllByItemIdAndPluggyAccountIdIn(UUID itemId, Collection<String> pluggyAccountIds);

    @Query(value = """
            SELECT pluggy_account_id
            FROM accounts
            WHERE pluggy_account_id IN (:pluggyAccountIds)
            """, nativeQuery = true)
    Set<String> findExistingPluggyAccountIdsIncludingDeleted(
            @Param("pluggyAccountIds") Collection<String> pluggyAccountIds
    );

    @EntityGraph(attributePaths = {"item", "item.institution"})
    List<Account> findAllByItemUsuarioIdOrderByNameAsc(UUID userId);

    @EntityGraph(attributePaths = {"item", "item.institution"})
    List<Account> findAllByItemIdAndItemUsuarioIdOrderByNameAsc(UUID itemId, UUID userId);

    @EntityGraph(attributePaths = {"item", "item.institution"})
    List<Account> findAllByItemInstitutionIdAndItemUsuarioIdOrderByNameAsc(
            UUID institutionId,
            UUID userId
    );
}
