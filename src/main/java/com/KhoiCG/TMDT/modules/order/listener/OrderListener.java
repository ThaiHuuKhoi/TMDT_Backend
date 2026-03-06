package com.KhoiCG.TMDT.modules.order.listener;

import com.KhoiCG.TMDT.modules.order.dto.PaymentSuccessEvent;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
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
        log.info("Received Webhook Kafka message: {}", message);
        try {
            PaymentSuccessEvent event = objectMapper.readValue(message, PaymentSuccessEvent.class);
            String sessionId = event.getSessionId();

            log.info("Xử lý đơn hàng từ Webhook cho Session: {}", sessionId);
            orderService.confirmOrderPayment(sessionId, Payment.PaymentMethod.STRIPE);

        } catch (Exception e) {
            log.error("Lỗi Kafka: {}", e.getMessage());
        }
    }
}