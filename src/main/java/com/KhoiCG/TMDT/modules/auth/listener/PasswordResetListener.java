package com.KhoiCG.TMDT.modules.auth.listener;

import com.KhoiCG.TMDT.modules.auth.event.PasswordResetRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetListener {

    private final JavaMailSender mailSender;

    // Lấy URL frontend từ file cấu hình (application.yml)
    @Value("${app.frontend.url:http://localhost:3002}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async // CỰC KỲ QUAN TRỌNG: Chạy luồng riêng biệt để API trả về kết quả ngay lập tức
    @EventListener
    public void handlePasswordResetRequestedEvent(PasswordResetRequestedEvent event) {
        log.info("Bắt đầu gửi email khôi phục mật khẩu cho: {}", event.getEmail());

        // Tạo đường link chứa token dẫn về trang Reset Password của Next.js
        String resetUrl = frontendUrl + "/reset-password?token=" + event.getToken();

        // Soạn nội dung Email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.getEmail());
        message.setSubject("Yêu cầu khôi phục mật khẩu - TrendLama");
        message.setText("Chào bạn,\n\n" +
                "Bạn nhận được email này vì đã có yêu cầu khôi phục mật khẩu cho tài khoản của bạn.\n" +
                "Vui lòng click vào đường link bên dưới để đặt lại mật khẩu (Link này có hiệu lực trong 30 phút):\n\n" +
                resetUrl + "\n\n" +
                "Nếu bạn không yêu cầu đổi mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.\n\n" +
                "Trân trọng,\nĐội ngũ TrendLama");

        try {
            mailSender.send(message); // Thực hiện gửi mail qua SMTP
            log.info("Đã gửi email khôi phục thành công tới: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email khôi phục mật khẩu cho {}: ", event.getEmail(), e);
        }
    }
}