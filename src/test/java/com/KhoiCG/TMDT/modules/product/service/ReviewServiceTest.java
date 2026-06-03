package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.product.dto.ReviewResponseDto;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.event.ReviewCreatedEvent;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepo userRepo;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private User mockUser;
    private Product mockProduct;
    private Review mockReview;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("Khách hàng A").build();
        mockProduct = Product.builder().id(100L).name("Laptop Gaming").build();

        mockReview = Review.builder()
                .id(10L)
                .user(mockUser)
                .product(mockProduct)
                .rating(5)
                .comment("Sản phẩm quá tuyệt vời!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // 1. TEST TẠO ĐÁNH GIÁ (CREATE)
    // ==========================================

    @Test
    @DisplayName("Tạo đánh giá: Thành công, lưu DB và phát Event tính điểm trung bình")
    void createReview_Success() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(orderRepository.existsPurchasedProduct(eq(1L), eq(100L), any())).thenReturn(true);
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);

        ReviewResponseDto result = reviewService.createReview(1L, 100L, 5, "Sản phẩm quá tuyệt vời!");

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Sản phẩm quá tuyệt vời!", result.getComment());
        assertEquals("Khách hàng A", result.getUserName());
        assertEquals(10L, result.getId());

        // Kiểm tra xem sự kiện có được phát ra đúng với Product ID không
        ArgumentCaptor<ReviewCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(100L, eventCaptor.getValue().getProductId());
    }

    @Test
    @DisplayName("Tạo đánh giá: Ném lỗi nếu người dùng đã đánh giá sản phẩm này rồi")
    void createReview_Fail_AlreadyReviewed() {
        // Giả lập DB trả về true (đã từng đánh giá)
        when(reviewRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview(1L, 100L, 5, "Good!")
        );

        assertEquals("REVIEW_ALREADY_EXISTS", ex.getCode());
        assertEquals("Bạn đã đánh giá sản phẩm này rồi!", ex.getMessage());
        verify(reviewRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Tạo đánh giá: Ném lỗi nếu User không tồn tại")
    void createReview_Fail_UserNotFound() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview(1L, 100L, 5, "Good!")
        );

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals("Người dùng không tồn tại", ex.getMessage());
    }

    @Test
    @DisplayName("Tạo đánh giá: Ném lỗi nếu chưa mua sản phẩm")
    void createReview_Fail_PurchaseRequired() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));
        when(orderRepository.existsPurchasedProduct(eq(1L), eq(100L), any())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview(1L, 100L, 5, "Good!")
        );

        assertEquals("PURCHASE_REQUIRED", ex.getCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo đánh giá: Ném lỗi nếu Product không tồn tại")
    void createReview_Fail_ProductNotFound() {
        when(reviewRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview(1L, 100L, 5, "Good!")
        );

        assertEquals("PRODUCT_NOT_FOUND", ex.getCode());
        assertEquals("Sản phẩm không tồn tại", ex.getMessage());
    }

    // ==========================================
    // 2. TEST LẤY DANH SÁCH ĐÁNH GIÁ (READ)
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách đánh giá: Map chính xác từ Entity sang DTO")
    void getReviewsByProduct_Success() {
        when(reviewRepository.findByProductId(100L)).thenReturn(List.of(mockReview));

        List<ReviewResponseDto> results = reviewService.getReviewsByProduct(100L);

        assertEquals(1, results.size());

        ReviewResponseDto dto = results.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("Khách hàng A", dto.getUserName()); // Chắc chắn User name được map đúng
        assertEquals(5, dto.getRating());
        assertEquals("Sản phẩm quá tuyệt vời!", dto.getComment());
    }
}