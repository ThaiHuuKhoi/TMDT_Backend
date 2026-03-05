package com.KhoiCG.TMDT.modules.email.service;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;

public interface NotificationService {
    void sendOrderEmail(OrderCreatedEvent event);
    void sendWelcomeEmail(UserCreatedEvent event);
}