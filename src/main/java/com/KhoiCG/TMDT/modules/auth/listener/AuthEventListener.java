package com.KhoiCG.TMDT.modules.auth.listener;

import com.KhoiCG.TMDT.common.event.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.auth.event.UserRegisteredEvent;
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

            UserCreatedEvent kafkaEvent = UserCreatedEvent.builder()
                    .email(event.getUser().getEmail())
                    .username(event.getUser().getName())
                    .build();


            kafkaTemplate.send("user.created", kafkaEvent);
            log.info("Đã đẩy event tạo user lên Kafka: {}", event.getUser().getEmail());

        } catch (Exception e) {
            log.error("Lỗi khi gửi Kafka event user.created: {}", e.getMessage());
        }
    }
}