package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.WishList;
import com.KhoiCG.TMDT.modules.product.entity.WishListId;
import com.KhoiCG.TMDT.modules.product.mapper.ProductMapper;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.WishListRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishListServiceTest {

    @Mock private WishListRepository wishListRepository;
    @Mock private UserRepo userRepo;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private WishListService wishListService;

    private User mockUser;
    private Product mockProduct;
    private WishList mockWishList;
    private ProductResponse mockProductResponse;
    private WishListId mockWishListId;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).build();
        mockProduct = Product.builder().id(100L).name("Bàn phím cơ").build();
        mockWishListId = new WishListId(1L, 100L);

        mockWishList = WishList.builder()
                .id(mockWishListId)
                .user(mockUser)
                .product(mockProduct)
                .build();

        mockProductResponse = ProductResponse.builder()
                .id(100L)
                .name("Bàn phím cơ")
                .build();
    }

    // ==========================================
    // 1. TEST TÍNH NĂNG TOGGLE (THÊM / XÓA)
    // ==========================================

    @Test
    @DisplayName("Toggle Wishlist: Xóa khỏi danh sách nếu đã tồn tại")
    void toggleWishlist_Remove_Success() {
        // Arrange: Giả lập là sản phẩm này đã có trong wishlist
        when(wishListRepository.existsById(mockWishListId)).thenReturn(true);

        // Act
        String result = wishListService.toggleWishlist(1L, 100L);

        // Assert
        assertEquals("Đã xóa khỏi danh sách yêu thích", result);
        verify(wishListRepository, times(1)).deleteById(mockWishListId);
        verify(wishListRepository, never()).save(any()); // Đảm bảo không gọi hàm save
    }

    @Test
    @DisplayName("Toggle Wishlist: Thêm mới vào danh sách nếu chưa tồn tại")
    void toggleWishlist_Add_Success() {
        // Arrange: Giả lập là chưa có trong wishlist
        when(wishListRepository.existsById(mockWishListId)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));

        // Act
        String result = wishListService.toggleWishlist(1L, 100L);

        // Assert
        assertEquals("Đã thêm vào danh sách yêu thích", result);

        // Kiểm tra xem đối tượng WishList được lưu có đúng dữ liệu không
        ArgumentCaptor<WishList> captor = ArgumentCaptor.forClass(WishList.class);
        verify(wishListRepository, times(1)).save(captor.capture());

        WishList savedWishList = captor.getValue();
        assertEquals(1L, savedWishList.getUser().getId());
        assertEquals(100L, savedWishList.getProduct().getId());
    }

    @Test
    @DisplayName("Toggle Wishlist: Ném lỗi nếu User không tồn tại (Khi thêm mới)")
    void toggleWishlist_Add_Fail_UserNotFound() {
        when(wishListRepository.existsById(mockWishListId)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                wishListService.toggleWishlist(1L, 100L)
        );

        assertEquals("User not found", ex.getMessage());
        verify(wishListRepository, never()).save(any());
    }

    @Test
    @DisplayName("Toggle Wishlist: Ném lỗi nếu Product không tồn tại (Khi thêm mới)")
    void toggleWishlist_Add_Fail_ProductNotFound() {
        when(wishListRepository.existsById(mockWishListId)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                wishListService.toggleWishlist(1L, 100L)
        );

        assertEquals("Product not found", ex.getMessage());
        verify(wishListRepository, never()).save(any());
    }

    // ==========================================
    // 2. TEST LẤY DANH SÁCH YÊU THÍCH (READ)
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách yêu thích: Phân trang và map sang ProductResponse thành công")
    void getUserWishlist_Success() {
        // Arrange
        Page<WishList> mockPage = new PageImpl<>(List.of(mockWishList));
        when(wishListRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        // Act
        Page<ProductResponse> result = wishListService.getUserWishlist(1L, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Bàn phím cơ", result.getContent().get(0).getName());

        // Đảm bảo Repository được gọi đúng tham số
        verify(wishListRepository, times(1)).findByUserId(eq(1L), any(Pageable.class));
    }
}