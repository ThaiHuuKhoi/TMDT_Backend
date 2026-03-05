package com.KhoiCG.TMDT.modules.email.listener;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.email.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService; // Dùng Interface (DIP)
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.created", groupId = "email-service")
    public void handleUserCreated(String rawJson) {
        try {
            UserCreatedEvent event = objectMapper.readValue(rawJson, UserCreatedEvent.class);
            if (event.getEmail() != null) {
                notificationService.sendWelcomeEmail(event); // Gọi qua Interface
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý User Event: ", e);
        }
    }

    @KafkaListener(topics = "order.created", groupId = "email-service")
    public void handleOrderCreated(String rawJson) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(rawJson, OrderCreatedEvent.class);
            if (event.getEmail() != null) {
                notificationService.sendOrderEmail(event); // Gọi qua Interface
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý Order Event: ", e);
        }
    }
}