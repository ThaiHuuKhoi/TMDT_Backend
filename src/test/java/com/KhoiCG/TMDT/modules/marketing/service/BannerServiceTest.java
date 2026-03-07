package com.KhoiCG.TMDT.modules.marketing.service;

import com.KhoiCG.TMDT.modules.marketing.dto.BannerRequest;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.entity.Banner;
import com.KhoiCG.TMDT.modules.marketing.entity.TargetType;
import com.KhoiCG.TMDT.modules.marketing.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerService bannerService;

    private Banner bannerProduct;
    private Banner bannerCategory;
    private Banner bannerExternal;

    @BeforeEach
    void setUp() {
        // Chuẩn bị 3 loại Banner để test hàm Switch Expression
        bannerProduct = Banner.builder()
                .id(1L)
                .title("Sale Laptop")
                .targetType(TargetType.PRODUCT)
                .targetId(100L)
                .displayOrder(1)
                .isActive(true)
                .build();

        bannerCategory = Banner.builder()
                .id(2L)
                .title("Sale Điện thoại")
                .targetType(TargetType.CATEGORY)
                .targetId(50L)
                .displayOrder(2)
                .isActive(true)
                .build();

        bannerExternal = Banner.builder()
                .id(3L)
                .title("Shopee Link")
                .targetType(TargetType.EXTERNAL_LINK)
                .linkUrl("https://shopee.vn/sale")
                .displayOrder(3)
                .isActive(true)
                .build();
    }

    // ==========================================
    // 1. TEST HÀM GET ACTIVE BANNERS & MAPPING URL
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách Banner: Phải map chính xác Target URL theo từng loại TargetType")
    void getActiveBanners_SuccessAndMapsUrlCorrectly() {
        // Arrange
        when(bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(bannerProduct, bannerCategory, bannerExternal));

        // Act
        List<BannerResponse> responses = bannerService.getActiveBanners();

        // Assert
        assertEquals(3, responses.size());

        // Kiểm tra Banner Sản phẩm -> URL phải là /products/100
        assertEquals("/products/100", responses.get(0).getTargetUrl());

        // Kiểm tra Banner Danh mục -> URL phải là /categories/50
        assertEquals("/categories/50", responses.get(1).getTargetUrl());

        // Kiểm tra Banner Link ngoài -> URL phải giữ nguyên linkUrl
        assertEquals("https://shopee.vn/sale", responses.get(2).getTargetUrl());
    }

    // ==========================================
    // 2. TEST HÀM THÊM MỚI (SAVE)
    // ==========================================

    @Test
    @DisplayName("Thêm Banner: Ánh xạ chuẩn từ Request sang Entity và trả về Response")
    void saveBanner_Success() {
        // Arrange
        BannerRequest request = new BannerRequest();
        request.setTitle("Banner Tết");
        request.setDescription("Giảm 50%");
        request.setImageUrl("tet.jpg");
        request.setTargetType(TargetType.CATEGORY);
        request.setTargetId(99L);
        request.setDisplayOrder(1);

        // Bắt lại đối tượng Banner được truyền vào hàm save để kiểm tra
        when(bannerRepository.save(any(Banner.class))).thenAnswer(i -> {
            Banner b = i.getArgument(0);
            b.setId(10L); // Giả lập DB tự tăng ID
            return b;
        });

        // Act
        BannerResponse response = bannerService.saveBanner(request);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Banner Tết", response.getTitle());
        assertEquals("/categories/99", response.getTargetUrl()); // Hàm mapUrl vẫn phải hoạt động đúng

        // Đảm bảo mặc định isActive là true khi lưu mới
        ArgumentCaptor<Banner> bannerCaptor = ArgumentCaptor.forClass(Banner.class);
        verify(bannerRepository).save(bannerCaptor.capture());
        assertTrue(bannerCaptor.getValue().getIsActive());
    }

    // ==========================================
    // 3. TEST HÀM XÓA (DELETE)
    // ==========================================

    @Test
    @DisplayName("Xóa Banner: Xóa thành công khi ID tồn tại")
    void deleteBanner_Success() {
        // Arrange
        when(bannerRepository.existsById(1L)).thenReturn(true);

        // Act
        bannerService.deleteBanner(1L);

        // Assert
        verify(bannerRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Xóa Banner: Ném lỗi RuntimeException khi ID không tồn tại")
    void deleteBanner_Fail_NotFound() {
        // Arrange
        when(bannerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () -> {
            bannerService.deleteBanner(999L);
        });

        assertEquals("Banner không tồn tại!", ex.getMessage());

        // Đảm bảo lệnh xóa thực sự không bao giờ được gọi
        verify(bannerRepository, never()).deleteById(anyLong());
    }
}