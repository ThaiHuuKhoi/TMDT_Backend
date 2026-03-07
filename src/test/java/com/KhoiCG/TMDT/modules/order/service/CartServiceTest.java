package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.entity.Cart;
import com.KhoiCG.TMDT.modules.order.entity.CartItem;
import com.KhoiCG.TMDT.modules.order.repository.CartItemRepository;
import com.KhoiCG.TMDT.modules.order.repository.CartRepository;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private UserRepo userRepo;

    @InjectMocks
    private CartService cartService;

    private User mockUser;
    private Cart mockCart;
    private ProductVariant mockVariant;
    private CartItem mockCartItem;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("test@gmail.com").build();

        mockCart = Cart.builder()
                .id(100L)
                .user(mockUser)
                .items(new ArrayList<>()) // Dùng ArrayList để có thể add/remove trong test
                .build();

        mockVariant = ProductVariant.builder()
                .id(50L)
                .sku("VAR-01")
                .stockQuantity(10) // Có 10 sản phẩm trong kho
                .build();

        mockCartItem = CartItem.builder()
                .id(200L)
                .cart(mockCart)
                .variant(mockVariant)
                .quantity(2) // Đã có 2 sản phẩm trong giỏ
                .build();
    }

    // ==========================================
    // 1. TEST HÀM getOrCreateCart
    // ==========================================

    @Test
    @DisplayName("Lấy giỏ hàng: Trả về giỏ hàng nếu đã tồn tại")
    void getOrCreateCart_Exists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(userRepo, never()).findById(any()); // Không cần tìm User nữa
    }

    @Test
    @DisplayName("Lấy giỏ hàng: Tạo mới nếu chưa tồn tại")
    void getOrCreateCart_NotExists_CreateNew() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
        assertEquals(mockUser, result.getUser());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Lấy giỏ hàng: Ném lỗi nếu User không tồn tại")
    void getOrCreateCart_UserNotFound_ThrowsException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> cartService.getOrCreateCart(1L));
        assertEquals("Không tìm thấy User", ex.getMessage());
    }

    // ==========================================
    // 2. TEST HÀM addToCart
    // ==========================================

    @Test
    @DisplayName("Thêm vào giỏ: Báo lỗi nếu số lượng mua lớn hơn tồn kho")
    void addToCart_NotEnoughStock() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(variantRepository.findById(50L)).thenReturn(Optional.of(mockVariant)); // Tồn kho: 10

        // Khách đòi mua 15 cái
        Exception ex = assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 50L, 15));
        assertTrue(ex.getMessage().contains("Số lượng tồn kho không đủ"));
    }

    @Test
    @DisplayName("Thêm vào giỏ: Thêm mới hoàn toàn nếu sản phẩm chưa có trong giỏ")
    void addToCart_AddNewItem() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(variantRepository.findById(50L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.empty()); // Chưa có trong giỏ
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.addToCart(1L, 50L, 3);

        assertEquals(1, result.getItems().size()); // Giỏ hàng có 1 món
        assertEquals(3, result.getItems().get(0).getQuantity()); // Số lượng là 3
    }

    @Test
    @DisplayName("Thêm vào giỏ: Cộng dồn số lượng nếu sản phẩm đã có trong giỏ")
    void addToCart_ExistingItem_AddQuantity() {
        mockCart.getItems().add(mockCartItem); // Giỏ đang có sẵn 2 cái

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(variantRepository.findById(50L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.of(mockCartItem));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Mua thêm 3 cái
        Cart result = cartService.addToCart(1L, 50L, 3);

        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().get(0).getQuantity()); // 2 + 3 = 5
    }

    @Test
    @DisplayName("Thêm vào giỏ: Báo lỗi nếu tổng cộng dồn vượt quá tồn kho")
    void addToCart_ExistingItem_ExceedsStock() {
        mockCart.getItems().add(mockCartItem); // Đang có 2 cái trong giỏ
        // Tồn kho là 10

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(variantRepository.findById(50L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.of(mockCartItem));

        // Mua thêm 9 cái (Tổng = 11 > 10)
        Exception ex = assertThrows(RuntimeException.class, () -> cartService.addToCart(1L, 50L, 9));
        assertEquals("Vượt quá số lượng tồn kho!", ex.getMessage());
    }

    // ==========================================
    // 3. TEST HÀM updateItemQuantity
    // ==========================================

    @Test
    @DisplayName("Cập nhật số lượng: Xóa luôn khỏi giỏ nếu số lượng <= 0")
    void updateItemQuantity_ZeroOrNegative_RemovesItem() {
        mockCart.getItems().add(mockCartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.of(mockCartItem));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Cập nhật số lượng về 0
        Cart result = cartService.updateItemQuantity(1L, 50L, 0);

        assertTrue(result.getItems().isEmpty()); // Giỏ hàng trống rỗng
    }

    @Test
    @DisplayName("Cập nhật số lượng: Báo lỗi nếu số lượng mới lớn hơn tồn kho")
    void updateItemQuantity_ExceedsStock() {
        mockCart.getItems().add(mockCartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.of(mockCartItem));

        // Sửa số lượng thành 20 (Kho chỉ có 10)
        Exception ex = assertThrows(RuntimeException.class, () -> cartService.updateItemQuantity(1L, 50L, 20));
        assertEquals("Vượt quá số lượng tồn kho!", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật số lượng: Cập nhật thành công")
    void updateItemQuantity_Success() {
        mockCart.getItems().add(mockCartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 50L)).thenReturn(Optional.of(mockCartItem));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.updateItemQuantity(1L, 50L, 7);

        assertEquals(7, result.getItems().get(0).getQuantity());
    }

    // ==========================================
    // 4. TEST HÀM removeItem & clearCart
    // ==========================================

    @Test
    @DisplayName("Xóa sản phẩm: Xóa chính xác sản phẩm khỏi giỏ")
    void removeItem_Success() {
        mockCart.getItems().add(mockCartItem);

        // Thêm 1 sản phẩm khác vào giỏ để test xóa đúng món chưa
        ProductVariant variant2 = ProductVariant.builder().id(51L).build();
        CartItem item2 = CartItem.builder().variant(variant2).build();
        mockCart.getItems().add(item2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Xóa sản phẩm 50L
        Cart result = cartService.removeItem(1L, 50L);

        assertEquals(1, result.getItems().size());
        assertEquals(51L, result.getItems().get(0).getVariant().getId()); // Chỉ còn lại sản phẩm 51L
    }

    @Test
    @DisplayName("Làm sạch giỏ hàng: Xóa toàn bộ sản phẩm")
    void clearCart_Success() {
        mockCart.getItems().add(mockCartItem);
        mockCart.getItems().add(new CartItem()); // Có 2 món

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(mockCart));

        cartService.clearCart(1L);

        assertTrue(mockCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(mockCart);
    }
}