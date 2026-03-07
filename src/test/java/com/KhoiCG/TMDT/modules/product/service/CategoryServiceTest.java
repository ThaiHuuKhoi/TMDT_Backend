package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.entity.Category;
import com.KhoiCG.TMDT.modules.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category mockCategory;

    @BeforeEach
    void setUp() {
        // Dùng Constructor vì Entity Category hiện tại không có @Builder
        mockCategory = new Category(1L, "Điện thoại", "dien-thoai", "phone.png");
    }

    // ==========================================
    // 1. TEST LẤY DANH SÁCH (READ)
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách Danh mục: Trả về thành công toàn bộ dữ liệu")
    void getAllCategories_Success() {
        // Arrange
        Category laptopCategory = new Category(2L, "Laptop", "laptop", "laptop.png");
        when(categoryRepository.findAll()).thenReturn(List.of(mockCategory, laptopCategory));

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Điện thoại", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    // ==========================================
    // 2. TEST THÊM MỚI (CREATE)
    // ==========================================

    @Test
    @DisplayName("Thêm mới Danh mục: Lưu thành công và trả về Entity")
    void createCategory_Success() {
        // Arrange
        Category newCategory = new Category(null, "Máy tính bảng", "tablet", "tablet.png");

        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
            Category saved = i.getArgument(0);
            saved.setId(3L); // Giả lập DB tự gen ID
            return saved;
        });

        // Act
        Category result = categoryService.createCategory(newCategory);

        // Assert
        assertNotNull(result.getId());
        assertEquals(3L, result.getId());
        assertEquals("Máy tính bảng", result.getName());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(captor.capture());
    }

    // ==========================================
    // 3. TEST CẬP NHẬT (UPDATE)
    // ==========================================

    @Test
    @DisplayName("Cập nhật Danh mục: Thành công khi ID tồn tại")
    void updateCategory_Success() {
        // Arrange: Dữ liệu gửi lên để sửa (ví dụ đổi tên thành Smartphone)
        Category updateDetails = new Category(null, "Smartphone", null, null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Category updatedCategory = categoryService.updateCategory(1L, updateDetails);

        // Assert
        assertNotNull(updatedCategory);
        assertEquals("Smartphone", updatedCategory.getName()); // Tên phải được cập nhật
        assertEquals("dien-thoai", updatedCategory.getSlug()); // Các trường khác giữ nguyên

        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(mockCategory);
    }

    @Test
    @DisplayName("Cập nhật Danh mục: Ném lỗi nếu ID không tồn tại")
    void updateCategory_Fail_NotFound() {
        // Arrange
        Category updateDetails = new Category(null, "Smartphone", null, null);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () -> {
            categoryService.updateCategory(99L, updateDetails);
        });

        assertEquals("Category not found", ex.getMessage());
        // Chắc chắn lệnh save không được gọi để bảo vệ DB
        verify(categoryRepository, never()).save(any());
    }

    // ==========================================
    // 4. TEST XÓA (DELETE)
    // ==========================================

    @Test
    @DisplayName("Xóa Danh mục: Gọi thành công lệnh xóa theo ID")
    void deleteCategory_Success() {
        // Act
        categoryService.deleteCategory(1L);

        // Assert: Không có giá trị trả về, chỉ cần kiểm tra xem hàm deleteById có được kích hoạt đúng ID không
        verify(categoryRepository, times(1)).deleteById(1L);
    }
}