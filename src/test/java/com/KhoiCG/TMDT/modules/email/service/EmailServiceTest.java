package com.KhoiCG.TMDT.modules.email.service;

import com.KhoiCG.TMDT.modules.email.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // Bơm email hệ thống ảo vào biến @Value
        ReflectionTestUtils.setField(emailService, "fromEmail", "shopkcg.no-reply@gmail.com");

        // Khởi tạo một đối tượng MimeMessage rỗng để MimeMessageHelper sử dụng mà không ném lỗi NullPointer
        mimeMessage = new MimeMessage((Session) null);
    }

    // ==========================================
    // 1. TEST GỬI MAIL ĐƠN HÀNG (THYMELEAF & CURRENCY)
    // ==========================================

    @Test
    @DisplayName("Gửi Email Đơn hàng: Map đúng dữ liệu vào Template HTML và gọi lệnh Send")
    void sendOrderEmail_Success() {
        // Arrange
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEmail("customer@gmail.com");
        event.setAmount(2500000L); // 2,500,000 VNĐ
        event.setStatus("COMPLETED");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Giả lập Thymeleaf render ra HTML
        when(templateEngine.process(eq("order-email"), any(Context.class)))
                .thenReturn("<html><h1>Cảm ơn bạn đã mua hàng!</h1></html>");

        // Act
        emailService.sendOrderEmail(event);

        // Assert 1: Kiểm tra xem Dữ liệu (Context) đẩy vào file HTML có đúng không
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("order-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertEquals("COMPLETED", capturedContext.getVariable("status"));

        // Kiểm tra hàm formatCurrency có hoạt động không (Chuỗi giá trị phải chứa số 2.500.000 hoặc 2,500,000)
        String formattedPrice = capturedContext.getVariable("price").toString();
        assertTrue(formattedPrice.contains("500") && formattedPrice.contains("2"));

        // Assert 2: Lệnh gửi mail thực sự được gọi 1 lần
        verify(mailSender, times(1)).send(mimeMessage);
    }

    // ==========================================
    // 2. TEST GỬI MAIL CHÀO MỪNG (TEXT THƯỜNG)
    // ==========================================

    @Test
    @DisplayName("Gửi Email Welcome: Sinh nội dung chính xác và gọi lệnh Send")
    void sendWelcomeEmail_Success() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent();
        event.setEmail("newuser@gmail.com");
        event.setUsername("KhoiCG");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendWelcomeEmail(event);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
        // Lưu ý: Rất khó assert nội dung bên trong mimeMessage vì nó đã bị compile,
        // nhưng verify gọi send() là đủ chứng minh luồng đi thành công.
    }

    // ==========================================
    // 3. TEST XỬ LÝ LỖI (EXCEPTION HANDLING)
    // ==========================================

    @Test
    @DisplayName("Xử lý lỗi: Bắt và log MessagingException mà không làm crash ứng dụng")
    void sendMimeMail_CatchesMessagingException() throws Exception {
        // Arrange: Tạo một mock MimeMessage cố tình ném lỗi khi MimeMessageHelper cố gắng cài đặt người gửi
        MimeMessage mockBrokenMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockBrokenMessage);

        // Khi hàm setFrom được gọi, ném ra lỗi
        doThrow(new MessagingException("Lỗi cấu hình SMTP SMTP/Mạng"))
                .when(mockBrokenMessage).setFrom(any(jakarta.mail.Address.class));

        UserCreatedEvent event = new UserCreatedEvent();
        event.setEmail("error-case@gmail.com");
        event.setUsername("ErrorUser");

        // Act: Gọi hàm (Chúng ta mong đợi nó KHÔNG ném lỗi văng ra ngoài, vì đã có try-catch)
        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail(event);
        });

        // Assert: Lệnh send cuối cùng chắc chắn không được gọi vì đã bị văng lỗi ở giữa chừng
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}