package com.KhoiCG.TMDT.common.event;

import com.KhoiCG.TMDT.modules.order.dto.PaymentSuccessEvent;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper; // 👇 Import thư viện Jackson
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.successful", groupId = "order-group")
    public void handlePaymentSuccess(String message) {
        log.info("📩 Received Kafka message: {}", message);

        try {
            PaymentSuccessEvent event = objectMapper.readValue(message, PaymentSuccessEvent.class);

            log.info("✅ Parsed Event for User: {}", event.getEmail());
            orderService.createOrder(event);

        } catch (Exception e) {
            log.error("❌ Error parsing Kafka message: {}", e.getMessage());
        }
    }
}