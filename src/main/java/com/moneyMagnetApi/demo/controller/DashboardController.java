package com.moneyMagnetApi.demo.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneyMagnetApi.demo.dto.dashboard.response.CategoryExpenseResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.DashboardResponse;
import com.moneyMagnetApi.demo.dto.dashboard.response.MonthlyFinancialResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard", description = "Resumo financeiro para a tela inicial")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(
            summary = "Retorna o resumo do dashboard",
            description = "Calcula saldo, receitas, despesas, economia mensal, historico financeiro, categorias de despesa e contas vinculadas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dashboard retornado"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public DashboardResponse getDashboard(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "Ano de referencia")
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @Parameter(description = "Mes de referencia")
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        YearMonth now = YearMonth.now();
        YearMonth referenceMonth = YearMonth.of(
                year == null ? now.getYear() : year,
                month == null ? now.getMonthValue() : month
        );

        return dashboardService.getDashboard(usuarioDetails.getId(), referenceMonth);
    }
    
    @GetMapping("/expenses-category")
    @Operation(
            summary = "Retorna despesas agrupadas por categoria",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Agrupamento retornado"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public List<CategoryExpenseResponse> getExpensesCategory(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "Ano de referencia")
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @Parameter(description = "Mes de referencia")
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        YearMonth now = YearMonth.now();
        YearMonth referenceMonth = YearMonth.of(
                year == null ? now.getYear() : year,
                month == null ? now.getMonthValue() : month
        );
        
        UUID userId =  usuarioDetails.getId();
        
        return dashboardService.getExpensesByCategory(userId, referenceMonth);
    }

    @GetMapping("/financial-history")
    @Operation(
            summary = "Retorna historico financeiro mensal",
            description = "Lista receitas e despesas dos ultimos meses a partir do mes de referencia.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Historico retornado"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public List<MonthlyFinancialResponse> getFinancialHistory(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "Quantidade de meses no historico")
            @RequestParam(defaultValue = "12") Integer months,
            @Parameter(description = "Ano de referencia")
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @Parameter(description = "Mes de referencia")
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        YearMonth now = YearMonth.now();
        YearMonth referenceMonth = YearMonth.of(
                year == null ? now.getYear() : year,
                month == null ? now.getMonthValue() : month
        );

        return dashboardService.getFinancialHistoryPublic(
                usuarioDetails.getId(),
                referenceMonth,
                months
        );
    }
}
