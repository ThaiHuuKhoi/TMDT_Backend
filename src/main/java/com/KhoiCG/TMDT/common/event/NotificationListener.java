package com.KhoiCG.TMDT.common.event;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.email.service.EmailService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper; // Spring tự động có cái này

    // --- 1. Xử lý User Đăng ký ---
    @KafkaListener(topics = "user.created", groupId = "email-service")
    public void handleUserCreated(String rawJson) { // Nhận String
        try {
            log.info("📩 User Event JSON: {}", rawJson);

            // Tự tay biến String thành Object (An toàn tuyệt đối)
            UserCreatedEvent event = objectMapper.readValue(rawJson, UserCreatedEvent.class);

            if (event.getEmail() != null) {
                emailService.sendMail(
                        event.getEmail(),
                        "Welcome to ShopKCG",
                        "Xin chào " + event.getUsername() + ", tài khoản của bạn đã tạo thành công!"
                );
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý User Event: ", e);
        }
    }

    // --- 2. Xử lý Đơn hàng ---
    @KafkaListener(topics = "order.created", groupId = "email-service")
    public void handleOrderCreated(String rawJson) { // Nhận String
        try {
            log.info("📦 Order Event JSON: {}", rawJson);

            // Tự tay biến String thành Object
            OrderCreatedEvent event = objectMapper.readValue(rawJson, OrderCreatedEvent.class);

            if (event.getEmail() != null) {
                emailService.sendOrderConfirmation(
                        event.getEmail(),
                        event.getAmount(),
                        event.getStatus()
                );
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý Order Event: ", e);
        }
    }
}