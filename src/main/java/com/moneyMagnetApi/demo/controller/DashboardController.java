package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.response.CategoryTotalDTO;
import com.moneyMagnetApi.demo.dto.response.MonthlyTotalDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly")
    public List<MonthlyTotalDTO> getMonthlyTotals(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam int year,
            @RequestParam int semester
    ) {
        LocalDate initDate, endDate;

        if (semester == 1) {
            initDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 6, 30);
        } else {
            initDate = LocalDate.of(year, 7, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        return dashboardService.getMonthlyTotals(usuarioDetails.getId(), initDate, endDate);
    }

    @GetMapping("/category")
    public List<CategoryTotalDTO> getCategoryTotals(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestParam int year,
            @RequestParam int semester
    ) {
        LocalDate initDate, endDate;

        if (semester == 1) {
            initDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 6, 30);
        } else {
            initDate = LocalDate.of(year, 7, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        return dashboardService.getCategoryTotals(usuarioDetails.getId(), initDate, endDate);
    }
}
