package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUsuarioId(UUID transactionId, UUID userId);

    Page<Transaction> findByUsuarioId(UUID usuarioId, Pageable pageable);

    @Query("""
        SELECT t
        FROM transaction t
        WHERE t.usuario.id = :userId
          AND EXTRACT(YEAR FROM t.date) = :year
    """)
    List<Transaction> findAllByYear(@Param("userId") UUID userId,
                                    @Param("year") Integer year);

    @Query("""
        SELECT t
        FROM transaction t
        WHERE t.usuario.id = :userId
          AND EXTRACT(YEAR FROM t.date) = :year
          AND EXTRACT(MONTH FROM t.date) = :month
    """)
    List<Transaction> findAllByYearAndMonth(@Param("userId") UUID userId,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month);
}
