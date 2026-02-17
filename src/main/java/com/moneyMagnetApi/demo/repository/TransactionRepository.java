package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.dto.response.MonthlyTotalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
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
          AND t.date BETWEEN :initDate AND :endDate
    """)
    List<Transaction> findMonthlyTotals(UUID userId, LocalDate initDate, LocalDate endDate);

}
