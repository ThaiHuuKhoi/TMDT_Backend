package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.entity.Category;
import com.KhoiCG.TMDT.modules.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 1. LƯU CACHE KHI LẤY DANH SÁCH
    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        log.info("🚀 [CACHE MISS] - Đang query Database để lấy danh sách Danh mục...");
        return categoryRepository.findAll();
    }

    // 2. XÓA CACHE KHI THÊM MỚI
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category createCategory(Category category) {
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Danh mục vì có dữ liệu mới.");
        return categoryRepository.save(category);
    }

    // (Tùy chọn) Thêm hàm Update và Delete tương tự
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setName(categoryDetails.getName());
        // ... set các trường khác
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Danh mục do cập nhật.");
        return categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Danh mục do xóa.");
        categoryRepository.deleteById(id);
    }
}