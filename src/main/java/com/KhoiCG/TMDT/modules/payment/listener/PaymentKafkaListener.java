package com.KhoiCG.TMDT.modules.payment.listener;

import com.KhoiCG.TMDT.modules.payment.dto.ProductCreatedEvent;

import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaListener {

    private final StripeService stripeService;

    @KafkaListener(topics = "product.created", groupId = "payment-service")
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Received product.created: {}", event);
        stripeService.createStripeProduct(event);
    }

    @KafkaListener(topics = "product.deleted", groupId = "payment-service")
    public void handleProductDeleted(String productId) {
        // Lưu ý: Kafka gửi String id, cần clean dấu quote nếu có
        String cleanId = productId.replaceAll("^\"|\"$", "");
        log.info("Received product.deleted: {}", cleanId);
        stripeService.deleteStripeProduct(cleanId);
    }
}