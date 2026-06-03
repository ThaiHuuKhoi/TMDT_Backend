package com.KhoiCG.TMDT.modules.email.listener;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.email.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new NotificationListener(notificationService, objectMapper);
    }

    @Test
    @DisplayName("handleUserCreated: valid payload sends welcome email")
    void handleUserCreated_valid_sendsWelcome() throws Exception {
        var event = new UserCreatedEvent();
        event.setEmail("user@example.com");
        event.setUsername("alice");
        String json = objectMapper.writeValueAsString(event);

        listener.handleUserCreated(json);

        verify(notificationService).sendWelcomeEmail(any(UserCreatedEvent.class));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("handleUserCreated: invalid JSON does not call service")
    void handleUserCreated_invalidJson_skips() throws Exception {
        listener.handleUserCreated("not-json");

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handleUserCreated: invalid email skips send")
    void handleUserCreated_invalidEmail_skips() throws Exception {
        var event = new UserCreatedEvent();
        event.setEmail("not-an-email");
        event.setUsername("alice");
        listener.handleUserCreated(objectMapper.writeValueAsString(event));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handleUserCreated: MessagingException is wrapped for retry/DLT")
    void handleUserCreated_sendFailure_wraps() throws Exception {
        var event = new UserCreatedEvent();
        event.setEmail("user@example.com");
        event.setUsername("alice");
        String json = objectMapper.writeValueAsString(event);
        doThrow(new MessagingException("smtp down")).when(notificationService).sendWelcomeEmail(any());

        assertThrows(IllegalStateException.class, () -> listener.handleUserCreated(json));

        verify(notificationService).sendWelcomeEmail(any(UserCreatedEvent.class));
    }

    @Test
    @DisplayName("handleOrderCreated: valid payload sends order email")
    void handleOrderCreated_valid_sendsOrderMail() throws Exception {
        var event = new OrderCreatedEvent();
        event.setEmail("buyer@example.com");
        event.setAmount(99_000L);
        event.setStatus("PENDING");
        String json = objectMapper.writeValueAsString(event);

        listener.handleOrderCreated(json);

        verify(notificationService).sendOrderEmail(any(OrderCreatedEvent.class));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("handleOrderCreated: MessagingException is wrapped for retry/DLT")
    void handleOrderCreated_sendFailure_wraps() throws Exception {
        var event = new OrderCreatedEvent();
        event.setEmail("buyer@example.com");
        event.setAmount(1L);
        event.setStatus("PENDING");
        String json = objectMapper.writeValueAsString(event);
        doThrow(new MessagingException("smtp down")).when(notificationService).sendOrderEmail(any());

        assertThrows(IllegalStateException.class, () -> listener.handleOrderCreated(json));

        verify(notificationService).sendOrderEmail(any(OrderCreatedEvent.class));
    }
}
