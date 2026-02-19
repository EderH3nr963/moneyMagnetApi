package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.response.CategoryTotalDTO;
import com.moneyMagnetApi.demo.dto.response.DashboardSummaryDTO;
import com.moneyMagnetApi.demo.dto.response.MonthlyTotalDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(
        name = "Dashboard",
        description = "Rotas para consumo do front end"
)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly")
    public List<MonthlyTotalDTO> getMonthlyTotals(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam int year
    ) {
        return dashboardService.getMonthlyTotals(usuarioDetails.getId(), year);
    }

    @GetMapping("/summary")
    public DashboardSummaryDTO getDashboardSummary(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam int year,
            @RequestParam int month
    ) {

        return dashboardService.summary(
                usuarioDetails.getId(),
                year,
                month
        );
    }

    @GetMapping("/category")
    public List<CategoryTotalDTO> getCategoryTotals(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam int year
    ) {


        return dashboardService.calculateCategoryTotals(usuarioDetails.getId(), year);
    }
}
