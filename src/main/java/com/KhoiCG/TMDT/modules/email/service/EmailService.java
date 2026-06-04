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

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendOrderEmail(OrderCreatedEvent event) throws MessagingException {
        String formattedPrice = formatCurrency(event.getAmount());

        Context context = new Context();
        context.setVariable("price", formattedPrice);
        context.setVariable("status", event.getStatus());
        context.setVariable("ordersUrl", frontendUrl + "/orders");

        String htmlContent = templateEngine.process("order-email", context);

        sendMimeMailOrThrow(event.getEmail(), "Xác nhận đơn hàng thành công - ShopKCG", htmlContent);
        log.info("Order Email sent to: {}", event.getEmail());
    }

    @Override
    public void sendWelcomeEmail(UserCreatedEvent event) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", event.getUsername());
        context.setVariable("shopUrl", frontendUrl);

        String htmlContent = templateEngine.process("welcome-email", context);
        sendMimeMailOrThrow(event.getEmail(), "Chào mừng bạn đến với ShopKCG!", htmlContent);
        log.info("Welcome Email sent to: {}", event.getEmail());
    }

    @Override
    public void sendRegistrationOtpEmail(String toEmail, String name, String otpCode) throws MessagingException {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otpCode", otpCode);

        String htmlContent = templateEngine.process("otp-email", context);
        sendMimeMailOrThrow(toEmail, "Mã OTP xác minh đăng ký - ShopKCG", htmlContent);
        log.info("Registration OTP Email sent to: {}", toEmail);
    }

    private void sendMimeMailOrThrow(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }

    private String formatCurrency(Long amount) {
        long value = amount != null ? amount : 0L;
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(value);
    }

}
