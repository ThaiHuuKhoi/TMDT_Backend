package com.KhoiCG.TMDT.modules.email.service;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import jakarta.mail.MessagingException;

public interface NotificationService {
    void sendOrderEmail(OrderCreatedEvent event) throws MessagingException;
    void sendWelcomeEmail(UserCreatedEvent event) throws MessagingException;
    void sendRegistrationOtpEmail(String toEmail, String name, String otpCode) throws MessagingException;
}