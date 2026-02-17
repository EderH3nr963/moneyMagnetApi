package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.request.CreateCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateNameCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateTypeCategoryDTO;
import com.moneyMagnetApi.demo.dto.response.CategoryResponseDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@Tag(
        name="Categoria",
        description = "Rotas de manipulação de categorias"
)
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @RequestBody CreateCategoryDTO dto
    ) {
        UUID usuarioId = userDetails.getId();
        CategoryResponseDTO category = categoryService.create(usuarioId, dto);
        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails
    ) {
        UUID usuarioId = userDetails.getId();
        List<CategoryResponseDTO> categories = categoryService.getAll(usuarioId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID categoryId
    ) {
        UUID usuarioId = userDetails.getId();
        CategoryResponseDTO category = categoryService.getById(usuarioId, categoryId);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID categoryId,
            @RequestBody UpdateCategoryDTO dto
    ) {
        UUID usuarioId = userDetails.getId();
        CategoryResponseDTO category = categoryService.update(usuarioId, categoryId, dto);
        return ResponseEntity.ok(category);
    }


    @PatchMapping("/{categoryId}/name")
    public ResponseEntity<CategoryResponseDTO> updateCategoryName(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID categoryId,
            @RequestBody UpdateNameCategoryDTO dto
    ) {
        UUID usuarioId = userDetails.getId();
        CategoryResponseDTO category = categoryService.updateName(usuarioId, categoryId, dto);
        return ResponseEntity.ok(category);
    }

    @PatchMapping("/{categoryId}/type")
    public ResponseEntity<CategoryResponseDTO> updateCategoryType(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID categoryId,
            @RequestBody UpdateTypeCategoryDTO dto
    ) {
        UUID usuarioId = userDetails.getId();
        CategoryResponseDTO category = categoryService.updateType(usuarioId, categoryId, dto);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @AuthenticationPrincipal UsuarioDetailsImpl userDetails,
            @PathVariable UUID categoryId
    ) {
        UUID usuarioId = userDetails.getId();
        categoryService.delete(usuarioId, categoryId);
    }
}
