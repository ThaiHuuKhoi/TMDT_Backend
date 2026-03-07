package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private ProductVariant variant1;
    private ProductVariant variant2;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        // Trong kho có 10 cái Điện thoại
        variant1 = ProductVariant.builder()
                .id(1L)
                .sku("PHONE-01")
                .stockQuantity(10)
                .build();

        // Trong kho có 5 cái Laptop
        variant2 = ProductVariant.builder()
                .id(2L)
                .sku("LAPTOP-01")
                .stockQuantity(5)
                .build();

        // Khách mua 2 Điện thoại
        item1 = OrderItem.builder()
                .variant(variant1)
                .productName("Điện thoại iPhone")
                .quantity(2)
                .build();

        // Khách mua 1 Laptop
        item2 = OrderItem.builder()
                .variant(variant2)
                .productName("Laptop Gaming")
                .quantity(1)
                .build();
    }

    // ==========================================
    // 1. TEST TRỪ KHO THÀNH CÔNG
    // ==========================================

    @Test
    @DisplayName("Trừ kho: Thành công khi số lượng mua hợp lệ, lưu đúng tồn kho mới")
    void deductInventoryForOrder_Success() {
        // Arrange
        List<OrderItem> items = List.of(item1, item2); // Khách mua 2 ĐT, 1 Laptop

        // Act
        inventoryService.deductInventoryForOrder(items);

        // Assert
        // ĐT: 10 - 2 = 8
        assertEquals(8, variant1.getStockQuantity());
        // Laptop: 5 - 1 = 4
        assertEquals(4, variant2.getStockQuantity());

        // Đảm bảo hàm save được gọi chính xác 2 lần cho 2 mặt hàng
        verify(variantRepository, times(2)).save(any(ProductVariant.class));
    }

    // ==========================================
    // 2. TEST LỖI HẾT HÀNG (OUT OF STOCK)
    // ==========================================

    @Test
    @DisplayName("Trừ kho: Ném lỗi RuntimeException và ngừng xử lý nếu có 1 món vượt quá tồn kho")
    void deductInventoryForOrder_Fail_OutOfStock() {
        // Arrange
        // Khách khăng khăng đòi mua 10 cái Laptop (trong khi kho chỉ có 5)
        item2.setQuantity(10);

        // Đưa món lỗi vào danh sách
        List<OrderItem> items = List.of(item1, item2);

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () -> {
            inventoryService.deductInventoryForOrder(items);
        });

        // Kiểm tra xem câu báo lỗi có đúng chuẩn thân thiện với người dùng không
        assertEquals("Sản phẩm 'Laptop Gaming' vừa hết hàng. Vui lòng liên hệ CSKH!", ex.getMessage());

        // Do sử dụng @Transactional, khi lỗi xảy ra ở giữa vòng lặp, toàn bộ tiến trình sẽ bị Rollback ở Database.
    }
}