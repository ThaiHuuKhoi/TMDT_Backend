package com.KhoiCG.TMDT.orderService.listener;

import com.KhoiCG.TMDT.orderService.dto.PaymentSuccessEvent;
import com.KhoiCG.TMDT.orderService.service.OrderService;
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
    private final ObjectMapper objectMapper; // 👇 Inject ObjectMapper để parse JSON

    @KafkaListener(topics = "payment.successful", groupId = "order-group")
    // 👇 SỬA: Nhận tham số là String thay vì Object để tránh lỗi Conversion
    public void handlePaymentSuccess(String message) {
        log.info("📩 Received Kafka message: {}", message);

        try {
            // 👇 Parse thủ công từ String sang Object
            PaymentSuccessEvent event = objectMapper.readValue(message, PaymentSuccessEvent.class);

            log.info("✅ Parsed Event for User: {}", event.getEmail());
            orderService.createOrder(event);

        } catch (Exception e) {
            log.error("❌ Error parsing Kafka message: {}", e.getMessage());
        }
    }
}