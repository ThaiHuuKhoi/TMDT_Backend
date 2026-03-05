package com.KhoiCG.TMDT.modules.auth.listener;

import com.KhoiCG.TMDT.modules.auth.event.UserRegisteredEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            // Map sang DTO dùng cho Kafka
            UserCreatedEvent kafkaEvent = new UserCreatedEvent();
            kafkaEvent.setEmail(event.getUser().getEmail());
            kafkaEvent.setUsername(event.getUser().getName());

            // Đẩy sang Kafka cho Email Service xử lý
            kafkaTemplate.send("user.created", kafkaEvent);
            log.info("Đã đẩy event tạo user lên Kafka cho email: {}", event.getUser().getEmail());

        } catch (Exception e) {
            // Nếu gửi Kafka thất bại, chỉ ghi log. Không làm rollback đơn đăng ký của user.
            log.error("Lỗi khi gửi Kafka event user.created: {}", e.getMessage());
        }
    }
}