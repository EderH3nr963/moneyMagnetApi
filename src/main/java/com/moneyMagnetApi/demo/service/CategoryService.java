package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.request.CreateCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateNameCategoryDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateTypeCategoryDTO;
import com.moneyMagnetApi.demo.dto.response.CategoryResponseDTO;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import com.moneyMagnetApi.demo.utils.StringNormalize;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CategoryResponseDTO create(UUID usuarioId, CreateCategoryDTO dto) {

        Usuario usuario = findUsuarioOrThrow(usuarioId);

        String normalizedName = StringNormalize.normalize(dto.name());

        if (categoryRepository.existsByNormalizedNameAndUsuarioId(normalizedName, usuarioId)) {
            throw new ValidationException("Já existe uma categoria com esse nome");
        }

        Category category = new Category();
        category.setName(dto.name());
        category.setNormalizedName(normalizedName);
        category.setType(dto.type());
        category.setColor(dto.color());
        category.setUsuario(usuario);

        categoryRepository.save(category);

        return CategoryResponseDTO.fromCategory(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAll(UUID usuarioId) {

        findUsuarioOrThrow(usuarioId);

        return categoryRepository.findAllByUsuarioId(usuarioId)
                .stream()
                .map(CategoryResponseDTO::fromCategory)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getById(UUID usuarioId, UUID categoryId) {

        Category category = findCategoryOrThrow(usuarioId, categoryId);

        return CategoryResponseDTO.fromCategory(category);
    }

    @Transactional
    public CategoryResponseDTO update(UUID usuarioId, UUID categoryId, UpdateCategoryDTO dto) {
        Category category = findCategoryOrThrow(usuarioId, categoryId);

        String normalizedName = StringNormalize.normalize(dto.name());

        if (!category.getNormalizedName().equals(normalizedName)
                && categoryRepository.existsByNormalizedNameAndUsuarioId(normalizedName, usuarioId)) {

            throw new ValidationException("Já existe uma categoria com esse nome");
        }

        category.setName(dto.name());
        category.setNormalizedName(normalizedName);
        category.setType(dto.type());
        category.setColor(dto.color());

        return CategoryResponseDTO.fromCategory(category);
    }

    @Transactional
    public CategoryResponseDTO updateName(UUID usuarioId, UUID categoryId, UpdateNameCategoryDTO dto) {

        Category category = findCategoryOrThrow(usuarioId, categoryId);

        String normalizedName = StringNormalize.normalize(dto.name());

        if (!category.getNormalizedName().equals(normalizedName)
                && categoryRepository.existsByNormalizedNameAndUsuarioId(normalizedName, usuarioId)) {

            throw new ValidationException("Já existe uma categoria com esse nome");
        }

        category.setName(dto.name());
        category.setNormalizedName(normalizedName);

        return CategoryResponseDTO.fromCategory(category);
    }

    @Transactional
    public CategoryResponseDTO updateType(UUID usuarioId, UUID categoryId, UpdateTypeCategoryDTO dto) {

        Category category = findCategoryOrThrow(usuarioId, categoryId);

        category.setType(dto.type());

        return CategoryResponseDTO.fromCategory(category);
    }

    @Transactional
    public void delete(UUID usuarioId, UUID categoryId) {

        Category category = findCategoryOrThrow(usuarioId, categoryId);

        categoryRepository.delete(category);
    }


    private Usuario findUsuarioOrThrow(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Usuário não encontrado ou inválido")
                );
    }

    private Category findCategoryOrThrow(UUID usuarioId, UUID categoryId) {
        return categoryRepository.findByIdAndUsuarioId(categoryId, usuarioId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Categoria não encontrada para o usuário informado")
                );
    }
}
