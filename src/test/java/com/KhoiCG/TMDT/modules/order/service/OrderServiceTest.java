package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.order.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.order.entity.*;
import com.KhoiCG.TMDT.modules.order.event.OrderCompletedEvent;
import com.KhoiCG.TMDT.modules.order.mapper.OrderMapper;
import com.KhoiCG.TMDT.modules.order.repository.CouponRepository;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.repository.PaymentRepository;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.service.InventoryService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // --- MOCK TOÀN BỘ DEPENDENCIES ---
    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private UserRepo userRepo;
    @Mock private StripeService stripeService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private InventoryService inventoryService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CouponService couponService;
    @Mock private CouponRepository couponRepository;

    @InjectMocks
    private OrderService orderService;

    // --- BIẾN DỮ LIỆU MẪU DÙNG CHUNG ---
    private User mockUser;
    private Cart mockCart;
    private Order mockPendingOrder;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("test@gmail.com").build();

        Product mockProduct = Product.builder().id(100L).name("Laptop Gaming").build();
        ProductVariant mockVariant = ProductVariant.builder()
                .product(mockProduct).sku("LAP01").price(BigDecimal.valueOf(20000000)).stockQuantity(5).build();

        CartItem mockCartItem = CartItem.builder().variant(mockVariant).quantity(1).build();
        mockCart = Cart.builder().user(mockUser).items(List.of(mockCartItem)).build();

        mockPendingOrder = Order.builder()
                .id(99L).user(mockUser).status(OrderStatus.PENDING).totalAmount(BigDecimal.valueOf(20000000))
                .stripeSessionId("sess_123").items(new ArrayList<>()).build();
    }

    // =========================================================
    // 1. TEST LUỒNG TẠO ĐƠN CHỜ (CREATE PENDING ORDER)
    // =========================================================

    @Test
    @DisplayName("Tạo PENDING Order: Thành công khi KHÔNG dùng mã giảm giá")
    void createPendingOrder_SuccessWithoutCoupon() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cartService.getOrCreateCart(1L)).thenReturn(mockCart);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order savedOrder = orderService.createPendingOrder(1L, "sess_123", null);

        assertNotNull(savedOrder);
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(0, BigDecimal.valueOf(20000000).compareTo(savedOrder.getTotalAmount()));
        assertEquals(1, savedOrder.getItems().size());

        // Đảm bảo không gọi Service xử lý Coupon
        verify(couponService, never()).applyCoupon(any());
    }

    @Test
    @DisplayName("Tạo PENDING Order: Thành công và áp dụng đúng mã giảm giá")
    void createPendingOrder_SuccessWithValidCoupon() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cartService.getOrCreateCart(1L)).thenReturn(mockCart);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Mock Coupon hợp lệ giảm 500k
        CouponResponse mockCouponResponse = new CouponResponse(true, "OK", 500000L, 19500000L);
        when(couponService.applyCoupon(any(CouponCheckRequest.class))).thenReturn(mockCouponResponse);

        Coupon mockCoupon = Coupon.builder().code("SALE500K").build();
        when(couponRepository.findByCode("SALE500K")).thenReturn(Optional.of(mockCoupon));

        Order savedOrder = orderService.createPendingOrder(1L, "sess_123", "SALE500K");

        // Kiểm tra tiền đã được trừ chuẩn xác chưa
        assertEquals(0, BigDecimal.valueOf(19500000).compareTo(savedOrder.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(savedOrder.getDiscountAmount()));
        assertEquals(mockCoupon, savedOrder.getCoupon());
    }

    @Test
    @DisplayName("Tạo PENDING Order: Thất bại vì kho không đủ hàng")
    void createPendingOrder_FailWhenOutOfStock() {
        // Cố tình sửa tồn kho = 0 nhưng giỏ hàng đòi mua 1
        mockCart.getItems().get(0).getVariant().setStockQuantity(0);

        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cartService.getOrCreateCart(1L)).thenReturn(mockCart);

        Exception ex = assertThrows(RuntimeException.class, () -> {
            orderService.createPendingOrder(1L, "sess_123", null);
        });

        assertTrue(ex.getMessage().contains("không đủ số lượng"));
        verify(orderRepository, never()).save(any());
    }

    // =========================================================
    // 2. TEST LUỒNG XÁC NHẬN THANH TOÁN (CONFIRM PAYMENT)
    // =========================================================

    @Test
    @DisplayName("Xác nhận thanh toán: Thành công toàn luồng (Trừ kho, Lưu Order, Lưu Payment, Phát Event)")
    void confirmOrderPayment_SuccessFlow() {
        // Arrange
        when(paymentRepository.findByTransactionId("sess_123")).thenReturn(Optional.empty()); // Chưa thanh toán
        when(orderRepository.findByStripeSessionId("sess_123")).thenReturn(Optional.of(mockPendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Order completedOrder = orderService.confirmOrderPayment("sess_123", Payment.PaymentMethod.STRIPE);

        // Assert
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());

        // 1. Phải gọi hàm trừ kho
        verify(inventoryService, times(1)).deductInventoryForOrder(mockPendingOrder.getItems());

        // 2. Phải lưu lịch sử Payment thành công
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(Payment.PaymentStatus.SUCCESS, paymentCaptor.getValue().getStatus());

        // 3. Phải phát Event báo đơn thành công để gửi Email
        verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("Xác nhận thanh toán: Chống trùng lặp (Idempotency) - Bỏ qua nếu giao dịch đã xử lý")
    void confirmOrderPayment_IdempotencyCheck() {
        // Arrange: Giả sử Stripe vô tình gọi Webhook 2 lần, DB đã lưu Payment này rồi
        Payment existingPayment = new Payment();
        when(paymentRepository.findByTransactionId("sess_123")).thenReturn(Optional.of(existingPayment));

        Order completedOrder = Order.builder().status(OrderStatus.COMPLETED).build();
        when(orderRepository.findByStripeSessionId("sess_123")).thenReturn(Optional.of(completedOrder));

        // Act
        Order result = orderService.confirmOrderPayment("sess_123", Payment.PaymentMethod.STRIPE);

        // Assert: KHÔNG BAO GIỜ được trừ kho hay phát Event lại lần 2
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        verify(inventoryService, never()).deductInventoryForOrder(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Xác nhận thanh toán: Nếu có lỗi xảy ra (VD: Lỗi trừ kho), phải ghi nhận Payment FAILED")
    void confirmOrderPayment_ExceptionSavesFailedPayment() {
        // Arrange
        when(paymentRepository.findByTransactionId("sess_FAIL")).thenReturn(Optional.empty());
        when(orderRepository.findByStripeSessionId("sess_FAIL")).thenReturn(Optional.of(mockPendingOrder));

        // Ép InventoryService ném lỗi (Ví dụ: Lúc nãy check có hàng, lúc thanh toán xong lại hết hàng)
        doThrow(new RuntimeException("Lỗi đồng bộ kho!")).when(inventoryService).deductInventoryForOrder(any());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () -> {
            orderService.confirmOrderPayment("sess_FAIL", Payment.PaymentMethod.STRIPE);
        });

        assertTrue(ex.getMessage().contains("Xử lý thanh toán thất bại"));

        // Quan trọng nhất: Phải kiểm tra xem hệ thống có lưu lại vé FAILED để đối soát không
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(Payment.PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());
    }
}