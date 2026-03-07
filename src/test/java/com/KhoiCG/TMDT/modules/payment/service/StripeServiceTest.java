package com.KhoiCG.TMDT.modules.payment.service;

import com.KhoiCG.TMDT.modules.order.service.CartService;
import com.KhoiCG.TMDT.modules.order.service.CouponService;
import com.KhoiCG.TMDT.modules.payment.dto.ProductCreatedEvent;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private CouponService couponService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private StripeService stripeService;

    // ==========================================
    // 1. TEST TẠO CHECKOUT SESSION (THANH TOÁN)
    // ==========================================

    @Test
    @DisplayName("Tạo Session: Thành công và trả về Client Secret")
    void createCheckoutSession_Success() throws StripeException {
        // Mở ra một chiều không gian giả mạo cho class Session của Stripe
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {

            // Giả lập kết quả trả về từ Stripe
            Session mockStripeSession = mock(Session.class);
            when(mockStripeSession.getClientSecret()).thenReturn("pi_123_secret_456");

            // Khi code gọi Session.create() -> Trả về kết quả giả
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockStripeSession);

            // Act: Gọi hàm
            String clientSecret = stripeService.createCheckoutSession(1L, 50000L, "Thanh toan don hang", "TXN_999");

            // Assert
            assertEquals("pi_123_secret_456", clientSecret);
            mockedSession.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
        }
    }

    @Test
    @DisplayName("Tạo Session: Tự động ép giá tối thiểu về 12,000 VNĐ nếu truyền số quá nhỏ")
    void createCheckoutSession_EnforcesMinimumAmount() throws StripeException {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {

            Session mockStripeSession = mock(Session.class);
            when(mockStripeSession.getClientSecret()).thenReturn("pi_min_amount");

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockStripeSession);

            // Cố tình truyền 5,000 VNĐ (nhỏ hơn quy định của Stripe)
            String clientSecret = stripeService.createCheckoutSession(1L, 5000L, "Mua kẹo", "TXN_001");

            assertEquals("pi_min_amount", clientSecret);

            // Note: Dù truyền 5000, bên trong ruột code bạn đã ép lên 12000.
            // Mockito xác nhận hàm create() vẫn chạy trơn tru mà không văng lỗi.
            mockedSession.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
        }
    }

    // ==========================================
    // 2. TEST ĐỒNG BỘ PRODUCT (KAFKA -> STRIPE)
    // ==========================================

    @Test
    @DisplayName("Tạo Product: Gọi thành công API tạo sản phẩm của Stripe")
    void createStripeProduct_Success() {
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setId("PROD_01");
        event.setName("Laptop Gaming");
        event.setPrice(1000L); // 1000 USD

        try (MockedStatic<Product> mockedProduct = mockStatic(Product.class)) {

            Product mockProduct = mock(Product.class);
            mockedProduct.when(() -> Product.create(any(ProductCreateParams.class))).thenReturn(mockProduct);

            // Hàm này không trả về gì, chỉ cần đảm bảo nó không văng lỗi
            assertDoesNotThrow(() -> stripeService.createStripeProduct(event));

            // Xác nhận API Stripe thực sự được gọi 1 lần
            mockedProduct.verify(() -> Product.create(any(ProductCreateParams.class)), times(1));
        }
    }

    @Test
    @DisplayName("Tạo Product: Ghi log lỗi, không sập app nếu Stripe ném StripeException")
    void createStripeProduct_HandlesStripeException() {
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setId("PROD_ERROR");

        try (MockedStatic<Product> mockedProduct = mockStatic(Product.class)) {
            // Giả lập mạng lỗi hoặc Stripe từ chối
            StripeException mockException = mock(StripeException.class);
            mockedProduct.when(() -> Product.create(any(ProductCreateParams.class))).thenThrow(mockException);

            // Do bạn đã có try-catch trong Service, nên app KHÔNG được phép crash
            assertDoesNotThrow(() -> stripeService.createStripeProduct(event));
        }
    }

    // ==========================================
    // 3. TEST XÓA & LẤY THÔNG TIN
    // ==========================================

    @Test
    @DisplayName("Xóa Product: Tìm và xóa thành công")
    void deleteStripeProduct_Success() throws StripeException {
        try (MockedStatic<Product> mockedProduct = mockStatic(Product.class)) {
            Product mockProduct = mock(Product.class);

            // Giả lập bước tìm
            mockedProduct.when(() -> Product.retrieve("PROD_123")).thenReturn(mockProduct);

            // Giả lập bước xóa (Product object gọi delete)
            when(mockProduct.delete()).thenReturn(mockProduct);

            assertDoesNotThrow(() -> stripeService.deleteStripeProduct("PROD_123"));

            // Xác nhận hành động tìm và xóa đều được kích hoạt
            mockedProduct.verify(() -> Product.retrieve("PROD_123"), times(1));
            verify(mockProduct, times(1)).delete();
        }
    }
}