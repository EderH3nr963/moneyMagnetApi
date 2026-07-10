package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.category.request.CreateCategoryRequest;
import com.moneyMagnetApi.demo.dto.category.request.UpdateCategoryRequest;
import com.moneyMagnetApi.demo.dto.category.response.CategoryResponse;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.CategeoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Consulta e manutencao de categorias do usuario")
public class CategoryController {

    private final CategeoryService categoryService;

    @PostMapping
    @Operation(
            summary = "Cria uma categoria",
            description = "Cria uma categoria personalizada para o usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Categoria criada"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse category = categoryService.create(usuarioDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping
    @Operation(
            summary = "Lista categorias acessiveis",
            description = "Retorna categorias padrao do sistema e categorias criadas pelo usuario.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categorias retornadas"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<List<CategoryResponse>> findAll(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        return ResponseEntity.ok(categoryService.getAll(usuarioDetails.getId()));
    }

    @PutMapping("/{categoryId}")
    @Operation(
            summary = "Atualiza uma categoria",
            description = "Atualiza nome, cor ou icone de uma categoria criada pelo usuario.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
                    @ApiResponse(responseCode = "404", description = "Categoria nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da categoria") @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(
                categoryService.update(usuarioDetails.getId(), categoryId, request)
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove uma categoria",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Categoria removida"),
                    @ApiResponse(responseCode = "404", description = "Categoria nao encontrada"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public void delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @Parameter(description = "ID da categoria") @PathVariable UUID categoryId
    ) {
        categoryService.delete(usuarioDetails.getId(), categoryId);
    }
}
