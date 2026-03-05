package com.KhoiCG.TMDT.modules.email.service;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements NotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOrderEmail(OrderCreatedEvent event) {
        String formattedPrice = formatCurrency(event.getAmount());

        Context context = new Context();
        context.setVariable("price", formattedPrice);
        context.setVariable("status", event.getStatus());

        String htmlContent = templateEngine.process("order-email", context);

        sendMimeMail(event.getEmail(), "Xác nhận đơn hàng thành công - ShopKCG", htmlContent);
        log.info("📦 Order Email sent to: {}", event.getEmail());
    }

    @Override
    public void sendWelcomeEmail(UserCreatedEvent event) {
        String content = "Xin chào " + event.getUsername() + ", tài khoản của bạn đã tạo thành công!";
        sendMimeMail(event.getEmail(), "Welcome to ShopKCG", content);
        log.info("📩 Welcome Email sent to: {}", event.getEmail());
    }

    private void sendMimeMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String formatCurrency(Long amount) {
        double displayAmount = amount != null ? amount / 100.0 : 0.0;
        return NumberFormat.getCurrencyInstance(Locale.US).format(displayAmount);
    }

}