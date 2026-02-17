package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.response.TransactionImportResponseDTO;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.utils.StringNormalize;
import jakarta.mail.Multipart;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.DateFormatter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExcelService {
    private final CategoryRepository categoryRepository;

    public ExcelService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ExcelReadResult readExcel(Usuario usuario, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Arquivo deve ser .xlsx");
        }

        List<Transaction> validTransactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<Category> categories = categoryRepository.findAllByUsuarioId(usuario.getId());

        Map<String, Category> categoryMap = categories.stream().collect(
                Collectors.toMap(
                        c-> c.getNormalizedName(),
                        c-> c
                )
        );

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String description = formatter.formatCellValue(row.getCell(0));
                    String amountStr = formatter.formatCellValue(row.getCell(1));
                    String categoryName = formatter.formatCellValue(row.getCell(2)).toLowerCase();
                    String dateStr = formatter.formatCellValue(row.getCell(3));

                    if (description.isBlank())
                        throw new RuntimeException("Descrição vazia");

                    BigDecimal amount = new BigDecimal(amountStr.replace(",", "."));
                    if (amount.compareTo(BigDecimal.ZERO) <= 0)
                        throw new RuntimeException("Amount deve ser positivo");

                    Category category = categoryMap.get(StringNormalize.normalize(categoryName));
                    if (category == null)
                        throw new RuntimeException("Categoria não encontrada: " + categoryName);

                    LocalDate date = LocalDate.parse(dateStr);

                    Transaction transaction = new Transaction();
                    transaction.setDate(date);
                    transaction.setAmount(amount);
                    transaction.setDescription(description);
                    transaction.setCategory(category);
                    transaction.setUsuario(usuario);

                    validTransactions.add(transaction);

                } catch (Exception e) {
                    errors.add("Erro na linha " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return new ExcelReadResult(
                validTransactions,
                errors
        );
    }
}
