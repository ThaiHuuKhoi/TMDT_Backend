package com.KhoiCG.TMDT.paymentService.service;

import com.KhoiCG.TMDT.orderService.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.orderService.dto.CouponResponse;
import com.KhoiCG.TMDT.orderService.service.CouponService;
import com.KhoiCG.TMDT.paymentService.dto.CartItemDto;
import com.KhoiCG.TMDT.paymentService.dto.ProductCreatedEvent;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    private final CouponService couponService;

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
    public String createCheckoutSession(List<CartItemDto> items, String couponCode) throws StripeException {

        // A. TÍNH TỔNG TIỀN GỐC
        long totalAmount = 0;
        StringBuilder description = new StringBuilder("Thanh toán đơn hàng gồm: ");

        for (CartItemDto item : items) {
            // Lưu ý: Nếu price là 20.5 (USD) thì nhân 100 thành cent.
            // Nếu price là 50000 (VND) thì giữ nguyên (tuỳ đơn vị tiền tệ bạn chọn).
            // Ở đây giả sử bạn dùng USD như code cũ:
            long itemTotal = (long) (item.getPrice() * 100) * item.getQuantity();
            totalAmount += itemTotal;

            description.append(item.getName()).append(" x").append(item.getQuantity()).append(", ");
        }

        // B. XỬ LÝ MÃ GIẢM GIÁ
        if (couponCode != null && !couponCode.isEmpty()) {
            CouponCheckRequest checkReq = new CouponCheckRequest();
            checkReq.setCode(couponCode);
            // CouponService đang tính theo Double, ta convert sang Double để tính
            checkReq.setOrderAmount( totalAmount);

            CouponResponse couponRes = couponService.applyCoupon(checkReq);

            if (couponRes.isValid()) {
                // Cập nhật tổng tiền sau khi giảm
                // Lưu ý: CouponService trả về finalPrice, ta lấy dùng luôn
                totalAmount = couponRes.getFinalPrice().longValue();
                description.append(" [Đã giảm giá mã: ").append(couponCode).append("]");
            }
        }

        // Chặn tiền âm (Stripe yêu cầu tối thiểu ~50 cent)
        if (totalAmount < 50) totalAmount = 50;

        // C. TẠO SESSION STRIPE (Gửi 1 Line Item tổng)
        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd") // Đổi thành "vnd" nếu bạn dùng tiền Việt
                                .setUnitAmount(totalAmount)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Tổng đơn hàng TrendLama")
                                                .setDescription(description.toString())
                                                // .addImage("https://link-to-your-logo.png")
                                                .build()
                                )
                                .build()
                )
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED) // Chế độ nhúng
                .setReturnUrl("http://localhost:3002/return?session_id={CHECKOUT_SESSION_ID}") // URL quay về sau khi thanh toán
                .addLineItem(lineItem) // Chỉ add 1 item tổng
                .build();

        Session session = Session.create(params);
        return session.getClientSecret();
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

    // Helper: Retrieve Session Info
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}