package com.KhoiCG.TMDT.emailService.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j // Dùng để log ra console thay vì System.out.println
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Method gửi email Order với giao diện HTML
    public void sendOrderConfirmation(String to, Long amount, String status) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác nhận đơn hàng thành công - TrendLama");

            // 1. Format tiền tệ (Ví dụ: 2000 -> $20.00 hoặc 20.00 USD)
            double displayAmount = amount != null ? amount / 100.0 : 0.0;
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
            String formattedPrice = currencyFormatter.format(displayAmount);

            // 2. Nội dung HTML
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;">
                        <div style="background-color: #000; color: #fff; padding: 20px; text-align: center;">
                            <h2 style="margin: 0;">Cảm ơn bạn đã đặt hàng!</h2>
                        </div>
                        <div style="padding: 20px;">
                            <p>Xin chào,</p>
                            <p>Đơn hàng của bạn đã được hệ thống ghi nhận và thanh toán thành công.</p>
                            
                            <table style="width: 100%%; border-collapse: collapse; margin-top: 20px;">
                                <tr style="background-color: #f9f9f9;">
                                    <td style="padding: 10px; border: 1px solid #ddd;"><strong>Tổng thanh toán:</strong></td>
                                    <td style="padding: 10px; border: 1px solid #ddd; color: #2ecc71; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #ddd;"><strong>Trạng thái:</strong></td>
                                    <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                                </tr>
                            </table>

                            <p style="margin-top: 20px;">Chúng tôi sẽ sớm gửi thông tin vận chuyển đến bạn.</p>
                            <a href="http://localhost:3002/orders" style="display: inline-block; background-color: #000; color: #fff; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin-top: 10px;">Xem đơn hàng</a>
                        </div>
                        <div style="background-color: #f1f1f1; padding: 10px; text-align: center; font-size: 12px; color: #666;">
                            &copy; 2026 TrendLama. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(formattedPrice, status);

            helper.setText(htmlContent, true); // true = Bật chế độ HTML

            mailSender.send(message);
            log.info("ORDER EMAIL SENT to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

    public void sendMail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // true = cho phép HTML nếu cần

            mailSender.send(message);
            log.info("MESSAGE SENT to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }
}