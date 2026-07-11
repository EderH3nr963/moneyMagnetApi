package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.account.Account;
import com.moneyMagnetApi.demo.domain.account.AccountType;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.transaction.TransactionNature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdAndAccountItemUsuarioId(
            UUID transactionId,
            UUID userId
    );
    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.nature in :natures
    """)
    Page<Transaction> findAllByUserAndNatureIn(
            @Param("userId") UUID userId,
            @Param("natures") List<TransactionNature> natures,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.nature in :natures
      and coalesce(t.paymentDate, t.date) >= :startDate
    """)
    Page<Transaction> findAllByUserAndNatureInStartingAt(
            @Param("userId") UUID userId,
            @Param("natures") List<TransactionNature> natures,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.nature in :natures
      and coalesce(t.paymentDate, t.date) < :endDate
    """)
    Page<Transaction> findAllByUserAndNatureInEndingBefore(
            @Param("userId") UUID userId,
            @Param("natures") List<TransactionNature> natures,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.nature in :natures
      and coalesce(t.paymentDate, t.date) >= :startDate
      and coalesce(t.paymentDate, t.date) < :endDate
    """)
    Page<Transaction> findAllByUserAndNatureInBetween(
            @Param("userId") UUID userId,
            @Param("natures") List<TransactionNature> natures,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.account.item.institution.id = :institutionId
      and t.account.type = :accountType
      and t.nature in :natures
    """)
    @EntityGraph(attributePaths = {"account", "category"})
    Page<Transaction> findAllByUserAndInstitutionAndAccountType(
            @Param("userId") UUID userId,
            @Param("institutionId") UUID institutionId,
            @Param("accountType") AccountType accountType,
            @Param("natures") List<TransactionNature> natures,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.account.item.id = :itemId
      and t.account.type = :accountType
      and t.nature in :natures
    """)
    @EntityGraph(attributePaths = {"account", "category"})
    Page<Transaction> findAllByUserAndItemAndAccountType(
            @Param("userId") UUID userId,
            @Param("itemId") UUID itemId,
            @Param("accountType") AccountType accountType,
            @Param("natures") List<TransactionNature> natures,
            Pageable pageable
    );
    List<Transaction> findAllByAccountAndNatureIn(
            Account account,
            List<TransactionNature> natures
    );
    List<Transaction> findAllByAccountItemUsuarioIdAndMerchantIsNotNull(UUID userId);

    List<Transaction> findAllByPluggyTransactionIdIn(List<String> pluggyTransactionIds);

    @EntityGraph(attributePaths = {"account", "category"})
    List<Transaction> findTop6ByAccountItemUsuarioIdAndNatureInOrderByDateDescCreatedAtDesc(
            UUID userId,
            List<TransactionNature> natures
    );
    
    Optional<Transaction> findByPluggyTransactionId(String pluggyTransactionId);
    
    @Query("""
    select t
    from Transaction t
    where t.account.item.usuario.id = :userId
      and t.date >= :startDate
      and t.date < :endDate
    order by t.date desc
    """)
    @EntityGraph(attributePaths = {"account", "category"})
    List<Transaction> findAllByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
