package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.product.dto.CategoryResponse;
import com.KhoiCG.TMDT.modules.product.dto.CategoryUpsertRequest;
import com.KhoiCG.TMDT.modules.product.entity.Category;
import com.KhoiCG.TMDT.modules.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        log.debug("Loading categories from database");
        return categoryRepository.findAllActive().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryUpsertRequest request) {
        String slug = resolveSlug(request);
        if (slug != null && categoryRepository.existsBySlugActive(slug, null)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", "Slug đã tồn tại: " + slug);
        }
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setImage(request.getImage());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpsertRequest request) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục."));
        String slug = resolveSlug(request);
        if (slug != null && categoryRepository.existsBySlugActive(slug, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS", "Slug đã tồn tại: " + slug);
        }
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());
        category.setImage(request.getImage());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục."));
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private String resolveSlug(CategoryUpsertRequest request) {
        return (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim()
                : null;
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .image(category.getImage())
                .build();
    }
}