package com.KhoiCG.TMDT.modules.auth.listener;

import com.KhoiCG.TMDT.modules.auth.event.UserRegisteredEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.email.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            UserCreatedEvent emailEvent = new UserCreatedEvent();
            emailEvent.setEmail(event.getUser().getEmail());
            emailEvent.setUsername(event.getUser().getName());
            notificationService.sendWelcomeEmail(emailEvent);
            log.info("Đã gửi welcome email cho: {}", event.getUser().getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi welcome email: {}", e.getMessage());
        }
    }
}
