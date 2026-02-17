package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.category.CategoryType;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ExcelServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExcelService excelService;

    @Test
    void shouldReturnValidTransactions_whenExcelIsCorrect() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername("admin");
        usuario.setEmail("email");
        usuario.setPassword("password");

        // Intânciar Categoria
        Category category = new Category();
        category.setName("alimentacao");
        category.setNormalizedName("alimentacao");
        category.setType(CategoryType.DESPESA);

        Mockito.when(categoryRepository.findAllByUsuarioId(usuario.getId()))
                .thenReturn(List.of(category));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Description");
        header.createCell(1).setCellValue("Amount");
        header.createCell(2).setCellValue("Category");
        header.createCell(3).setCellValue("Date");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("Mercado");
        row.createCell(1).setCellValue("100.50");
        row.createCell(2).setCellValue("alimentacao");
        row.createCell(3).setCellValue("2024-01-10");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );

        var result = excelService.readExcel(usuario, file);

        assertEquals(1, result.transactions().size());
        assertTrue(result.errors().isEmpty());

        Transaction transaction = result.transactions().get(0);

        assertEquals("Mercado", transaction.getDescription());
        assertEquals(new BigDecimal("100.50"), transaction.getAmount());
        assertEquals(LocalDate.parse("2024-01-10"), transaction.getDate());
        assertEquals(category, transaction.getCategory());
    }

}
