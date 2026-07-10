package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.dto.dashboard.response.CategoryExpenseResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByIdAndUsuarioId(UUID id, UUID userId);
    
    @Query("""
    select c
    from Category c
    where c.id = :categoryId
      and (c.usuario.id = :userId or c.usuario is null)
    """)
    Optional<Category> findAccessibleById(
            @Param("categoryId") UUID categoryId,
            @Param("userId") UUID userId
    );
    
    boolean existsByNormalizedNameAndUsuarioId(String normalizedName, UUID userId);
    
    boolean existsByNormalizedNameAndUsuarioIdAndIdNot(
            String normalizedName,
            UUID userId,
            UUID id
    );
    
    List<Category> findAllByUsuarioIdOrderByNameAsc(UUID userId);
    
    List<Category> findAllByUsuarioIdOrUsuarioIsNullOrderByNameAsc(UUID userId);
    
    @Query("""
    select new com.moneyMagnetApi.demo.dto.dashboard.response.CategoryExpenseResponse(
            c.id,
            coalesce(c.name, 'Sem categoria'),
            sum(abs(t.amount)),
            coalesce(c.color, '#71717A')
        )
        from Transaction t
        left join t.category c
        where t.account.item.usuario.id = :userId
          and (
              t.nature = :nature
              or (
                  t.nature is null
                  and t.type = com.moneyMagnetApi.demo.domain.transaction.TransactionType.DEBIT
                  and (
                      t.account.type <> com.moneyMagnetApi.demo.domain.account.AccountType.CREDIT
                      or lower(coalesce(t.providerCategory, '')) <> 'credit card payment'
                  )
                  and coalesce(c.normalizedName, '') <> 'transferencia-entre-contas'
              )
          )
          and t.status = :status
          and t.date >= :startDate
          and t.date < :endDate
        group by c.id, c.name, c.color
        order by sum(abs(t.amount)) desc
    """)
    List<CategoryExpenseResponse> findExpensesByCategory(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("nature") TransactionNature nature,
            @Param("status") TransactionStatus status
    );
}
