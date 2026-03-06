package com.KhoiCG.TMDT.modules.payment.service;

import com.KhoiCG.TMDT.modules.order.service.CartService;
import com.KhoiCG.TMDT.modules.order.service.CouponService;
import com.KhoiCG.TMDT.modules.payment.dto.ProductCreatedEvent;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    private final CouponService couponService;
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
    // Xóa đoạn gọi DB, tính tiền, gọi CartService, CouponService đi. Chỉ nhận các giá trị đã tính sẵn.
    // Thêm tham số txnRef
    public String createCheckoutSession(Long userId, Long finalAmountVND, String description, String txnRef) throws StripeException {

        if (finalAmountVND < 12000) finalAmountVND = 12000L;

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("vnd")
                                .setUnitAmount(finalAmountVND)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Thanh toán hóa đơn TrendLama")
                                                .setDescription(description)
                                                .build()
                                )
                                .build()
                )
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                .setReturnUrl("http://localhost:3002/payment/return?session_id={CHECKOUT_SESSION_ID}")
                .addLineItem(lineItem)
                .setClientReferenceId(txnRef) // <-- ĐỔI TỪ userId SANG txnRef Ở ĐÂY
                .build();

        return Session.create(params).getClientSecret();
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    private Long getStripeProductPrice(String productId) throws StripeException {
        var prices = Price.list(java.util.Map.of("product", productId, "limit", 1));

        if (prices.getData().isEmpty()) return 0L;
        return prices.getData().get(0).getUnitAmount();
    }

}