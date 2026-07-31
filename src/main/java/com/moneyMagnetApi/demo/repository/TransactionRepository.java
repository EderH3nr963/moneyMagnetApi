package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import com.moneyMagnetApi.demo.domain.transaction.TransactionStatus;
import com.moneyMagnetApi.demo.domain.transaction.TransactionType;
import com.moneyMagnetApi.demo.repository.projection.MonthlyFinancialProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
        Optional<Transaction> findByIdAndAccountItemUsuarioId(
                        UUID transactionId,
                        UUID userId);
        
        @Query(
                value = """
                        SELECT t.*
                        FROM transactions t
                        JOIN accounts a ON a.id = t.account_id
                        JOIN items i ON i.id = a.item_id
                        WHERE i.user_id = :userId
                          AND CAST(t.nature AS text) IN (:natures)
                          AND (:accountId IS NULL OR t.account_id = :accountId)
                          AND (CAST(:startDate AS date) IS NULL
                               OR CAST(t.payment_date AS date) >= CAST(:startDate AS date))
                          AND (CAST(:endDate AS date) IS NULL
                               OR CAST(t.payment_date AS date) <= CAST(:endDate AS date))
                          AND (
                            CAST(:search AS text) IS NULL OR
                            to_tsvector('portuguese', coalesce(t.description, ''))
                              @@ websearch_to_tsquery('portuguese', CAST(:search AS text))
                          )
                        ORDER BY t.payment_date DESC
                        """,
                countQuery = """
                        SELECT count(*)
                        FROM transactions t
                        JOIN accounts a ON a.id = t.account_id
                        JOIN items i ON i.id = a.item_id
                        WHERE i.user_id = :userId
                          AND CAST(t.nature AS text) IN (:natures)
                          AND (:accountId IS NULL OR t.account_id = :accountId)
                          AND (CAST(:startDate AS date) IS NULL
                               OR CAST(t.payment_date AS date) >= CAST(:startDate AS date))
                          AND (CAST(:endDate AS date) IS NULL
                               OR CAST(t.payment_date AS date) <= CAST(:endDate AS date))
                          AND (
                            CAST(:search AS text) IS NULL OR
                            to_tsvector('portuguese', coalesce(t.description, ''))
                              @@ websearch_to_tsquery('portuguese', CAST(:search AS text))
                          )
                        """,
                nativeQuery = true
        )
        Page<Transaction> findWithFilters(
                @Param("userId") UUID userId,
                @Param("natures") List<String> natures,
                @Param("accountId") UUID accountId,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate,
                @Param("search") String search,
                Pageable pageable
        );

        @Query("""
                        SELECT t
                        FROM Transaction t
                        WHERE t.account.item.usuario.id = :userId
                          AND t.account.item.institution.id = :institutionId
                          AND t.account.type = :accountType
                          AND t.nature IN :natures
        """)
        @EntityGraph(attributePaths = { "account", "category" })
        Page<Transaction> findAllByUserAndInstitutionAndAccountType(
                        @Param("userId") UUID userId,
                        @Param("institutionId") UUID institutionId,
                        @Param("accountType") AccountType accountType,
                        @Param("natures") List<TransactionNature> natures,
                        Pageable pageable);

        @Query("""
                        SELECT t
                        FROM Transaction t
                        WHERE t.account.item.usuario.id = :userId
                          AND t.account.item.id = :itemId
                          AND t.account.type = :accountType
                          AND t.nature IN :natures
        """)
        @EntityGraph(attributePaths = { "account", "category" })
        Page<Transaction> findAllByUserAndItemAndAccountType(
                        @Param("userId") UUID userId,
                        @Param("itemId") UUID itemId,
                        @Param("accountType") AccountType accountType,
                        @Param("natures") List<TransactionNature> natures,
                        Pageable pageable);

        List<Transaction> findAllByAccountAndNatureIn(
                        Account account,
                        List<TransactionNature> natures);

        List<Transaction> findAllByAccountItemUsuarioIdAndMerchantIsNotNull(UUID userId);

        List<Transaction> findAllByPluggyTransactionIdIn(List<String> pluggyTransactionIds);

        @EntityGraph(attributePaths = { "account", "category" })
        List<Transaction> findTop6ByAccountItemUsuarioIdAndNatureInOrderByDateDescCreatedAtDesc(
                        UUID userId,
                        List<TransactionNature> natures);

        Optional<Transaction> findByPluggyTransactionId(String pluggyTransactionId);
        
        @Query("""
                        SELECT t
                        FROM Transaction t
                        WHERE t.account.item.usuario.id = :userId
                          AND t.date >= :startDate
                          AND t.date < :endDate
                        order by t.date desc
        """)
        @EntityGraph(attributePaths = { "account", "category" })
        List<Transaction> findAllByUserAndPeriod(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
        
        @Query("""
            SELECT
                YEAR(t.date) AS year,
                MONTH(t.date) AS month,
        
                SUM(
                    CASE
                        WHEN t.type = :income
                        THEN ABS(t.amount)
                        ELSE 0
                    END
                ) AS income,
        
                SUM(
                    CASE
                        WHEN t.type = :expense
                        THEN ABS(t.amount)
                        ELSE 0
                    END
                ) AS expenses
        
            FROM Transaction t
            WHERE t.account.item.usuario.id = :userId
              AND t.paymentDate >= :startDate
              AND t.paymentDate < :endDate
        
            GROUP BY YEAR(t.date), MONTH(t.date)
            ORDER BY YEAR(t.date), MONTH(t.date)
        """)
        List<MonthlyFinancialProjection> findMonthlyFinancialSummary(
                UUID userId,
                LocalDateTime startDate,
                LocalDateTime endDate,
                TransactionType income,
                TransactionType expense
        );
}
