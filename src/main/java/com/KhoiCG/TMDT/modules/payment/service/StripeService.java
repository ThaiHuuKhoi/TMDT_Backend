package com.KhoiCG.TMDT.modules.payment.service;

import com.KhoiCG.TMDT.modules.order.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.order.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.order.entity.Cart;
import com.KhoiCG.TMDT.modules.order.entity.CartItem;
import com.KhoiCG.TMDT.modules.order.service.CartService;
import com.KhoiCG.TMDT.modules.order.service.CouponService;
import com.KhoiCG.TMDT.modules.payment.dto.CartItemDto;
import com.KhoiCG.TMDT.modules.payment.dto.ProductCreatedEvent;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    private final CouponService couponService;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;

    // --- 1. Tạo Product trên Stripe (Từ Kafka) ---
    public void createStripeProduct(ProductCreatedEvent event) {
        try {
            ProductCreateParams params = ProductCreateParams.builder()
                    .setId(event.getId()) // Sync ID database với ID Stripe
                    .setName(event.getName())
                    .setDefaultPriceData(
                            ProductCreateParams.DefaultPriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(event.getPrice() * 100) // Stripe tính bằng cent
                                    .build()
                    )
                    .build();

            Product.create(params);
            log.info("Synced product to Stripe: {}", event.getId());
        } catch (StripeException e) {
            log.error("Error creating Stripe product", e);
        }
    }

    // --- 2. Xóa Product trên Stripe ---
    public void deleteStripeProduct(String productId) {
        try {
            Product product = Product.retrieve(productId);
            // Stripe API delete object
            product.delete();
            log.info("Deleted product on Stripe: {}", productId);
        } catch (StripeException e) {
            log.error("Error deleting Stripe product", e);
        }
    }

    // --- 3. Tạo Checkout Session (Cho Controller gọi) ---
    public String createCheckoutSession(Long userId, String couponCode) throws StripeException {

        // 1. LẤY GIỎ HÀNG TỪ DATABASE DỰA TRÊN USER ID
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống!");
        }

        // 2. TÍNH TỔNG TIỀN TRỰC TIẾP TỪ DỮ LIỆU DATABASE (Bảo mật 100%)
        long totalAmountInCent = 0;
        StringBuilder description = new StringBuilder("Thanh toán đơn hàng: ");

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getVariant();

            // Tính tiền = giá DB * số lượng (Nhân 100 để ra cent cho Stripe)
            long itemTotal = variant.getPrice().multiply(BigDecimal.valueOf(100)).longValue() * item.getQuantity();
            totalAmountInCent += itemTotal;

            // Format mô tả: "Tên SP (Size M, Đen) x2"
            description.append(variant.getProduct().getName())
                    .append(" x").append(item.getQuantity()).append(", ");
        }

        // 3. XỬ LÝ MÃ GIẢM GIÁ (Nếu có)
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            CouponCheckRequest checkReq = new CouponCheckRequest();
            checkReq.setCode(couponCode);
            checkReq.setOrderAmount(totalAmountInCent);

            CouponResponse couponRes = couponService.applyCoupon(checkReq);
            if (couponRes.isValid()) {
                totalAmountInCent = couponRes.getFinalPrice();
                description.append(" [Áp dụng mã: ").append(couponCode).append("]");
            }
        }

        // Tối thiểu 0.5$ cho Stripe (tương đương 50 cent)
        if (totalAmountInCent < 50) totalAmountInCent = 50;

        // 4. TẠO SESSION STRIPE
        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(totalAmountInCent)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Thanh toán hóa đơn TrendLama")
                                                .setDescription(description.toString())
                                                .build()
                                )
                                .build()
                )
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                .setReturnUrl("http://localhost:3002/payment/return?session_id={CHECKOUT_SESSION_ID}") // Sửa lại URL return cho khớp frontend
                .addLineItem(lineItem)
                .build();

        return Session.create(params).getClientSecret();
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    // Helper: Lấy giá từ Stripe (Giống getStripeProductPrice trong code Node)
    private Long getStripeProductPrice(String productId) throws StripeException {
        // Tìm list prices của product
        // Code cũ: stripe.prices.list({ product: productId.toString() })
        // Java SDK logic tương tự
        // Lưu ý: Thực tế nên cache giá này hoặc lưu trong DB chính.
        var prices = Price.list(java.util.Map.of("product", productId, "limit", 1));

        if (prices.getData().isEmpty()) return 0L;
        return prices.getData().get(0).getUnitAmount();
    }

}