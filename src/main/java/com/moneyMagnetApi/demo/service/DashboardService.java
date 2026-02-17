package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.CategoryType;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.dto.response.CategoryTotalDTO;
import com.moneyMagnetApi.demo.dto.response.MonthlyTotalDTO;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Totais por mês
    public List<MonthlyTotalDTO> getMonthlyTotals(UUID userId, LocalDate initDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findMonthlyTotals(userId, initDate, endDate);

        Map<Integer, MonthlyTotalDTO> map = new HashMap<>();

        for (int month = initDate.getMonthValue(); month <= endDate.getMonthValue(); month++) {
            map.put(month, new MonthlyTotalDTO(
                    getMonthName(month),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
        }

        for (Transaction t : transactions) {
            int month = t.getDate().getMonthValue();
            MonthlyTotalDTO m = map.get(month);

            BigDecimal receita = m.receita();
            BigDecimal despesa = m.despesa();

            if (t.getCategory().getType().equals(CategoryType.RECEITA)) {
                receita = receita.add(t.getAmount());
            } else {
                despesa = despesa.add(t.getAmount());
            }

            map.put(month, new MonthlyTotalDTO(
                    getMonthName(month),
                    receita,
                    despesa,
                    receita.subtract(despesa)
            ));
        }

        // Ordena por mês
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    // Totais por categoria
    public List<CategoryTotalDTO> getCategoryTotals(UUID userId, LocalDate initDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findAll()
                .stream()
                .filter(t -> t.getUsuario().getId().equals(userId)
                        && !t.getDate().isBefore(initDate)
                        && !t.getDate().isAfter(endDate))
                .toList();

        Map<String, CategoryTotalDTO> map = new HashMap<>();

        for (Transaction t : transactions) {
            String category = t.getCategory().getName();
            CategoryType type =  t.getCategory().getType();
            BigDecimal amount = t.getAmount();
            map.put(category, map
                    .getOrDefault(category, new CategoryTotalDTO(category, amount, type)));
        }

        return map.entrySet().stream()
                .map(e -> new CategoryTotalDTO(e.getKey(), e.getValue().total(), e.getValue().type()))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();
    }

    private String getMonthName(int month) {
        return switch (month) {
            case 1 -> "Jan";
            case 2 -> "Fev";
            case 3 -> "Mar";
            case 4 -> "Abr";
            case 5 -> "Mai";
            case 6 -> "Jun";
            case 7 -> "Jul";
            case 8 -> "Ago";
            case 9 -> "Set";
            case 10 -> "Out";
            case 11 -> "Nov";
            case 12 -> "Dez";
            default -> "";
        };
    }
}
