package com.KhoiCG.TMDT.modules.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    // Một chuỗi Base64 chuẩn dùng làm Secret Key (Phải đủ độ dài cho HMAC-SHA256, ít nhất 32 bytes)
    // Chuỗi dưới đây là Base64 của: "MySuperSecretKeyForTmdtProjectWhichIsVeryLong"
    private final String VALID_SECRET = "TXlTdXBlclNlY3JldEtleUZvclRtZHRQcm9qZWN0V2hpY2hJc1ZlcnlMb25n";

    @BeforeEach
    void setUp() {
        // Khởi tạo trực tiếp thay vì dùng @InjectMocks để dễ dàng bơm @Value
        jwtService = new JwtService();

        // Bơm các giá trị ảo vào các trường @Value
        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000 * 60 * 60L); // 1 giờ
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 1000 * 60 * 60 * 24 * 7L); // 7 ngày
    }

    // ==========================================
    // 1. KỊCH BẢN THÀNH CÔNG (HAPPY PATH)
    // ==========================================

    @Test
    @DisplayName("Tạo và trích xuất: Token sinh ra phải chứa đúng thông tin Email (Subject)")
    void generateAndExtractToken_Success() {
        String email = "admin@tmdt.com";
        String token = jwtService.generateToken(email);

        assertNotNull(token);
        String extractedEmail = jwtService.extractUserName(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Xác thực: Trả về TRUE khi token hợp lệ và khớp với UserDetails")
    void validateToken_Success() {
        String email = "user@tmdt.com";
        String token = jwtService.generateToken(email);

        when(userDetails.getUsername()).thenReturn(email);

        boolean isValid = jwtService.validateToken(token, userDetails);
        assertTrue(isValid);
    }

    // ==========================================
    // 2. KỊCH BẢN TẤN CÔNG & LỖI BẢO MẬT
    // ==========================================

    @Test
    @DisplayName("Bảo mật: Báo lỗi ExpiredJwtException khi Token đã hết hạn")
    void extractUserName_Fail_WhenTokenExpired() {
        String email = "user@tmdt.com";

        // Cố tình chỉnh lại thời gian sống thành SỐ ÂM (Hết hạn ngay từ trong quá khứ)
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        String expiredToken = jwtService.generateToken(email);

        // JWT sẽ tự động ném lỗi khi cố đọc một token đã quá hạn
        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractUserName(expiredToken);
        });
    }

    @Test
    @DisplayName("Bảo mật: Báo lỗi SignatureException khi Token bị làm giả chữ ký (Hacker tự ký bằng Secret khác)")
    void validateToken_Fail_WhenSignatureIsTampered() {
        String email = "admin@tmdt.com";
        String token = jwtService.generateToken(email);

        // Tạo ra một JwtService thứ 2, đóng vai Hacker có một Secret Key khác
        JwtService hackerJwtService = new JwtService();
        String HACKER_SECRET = "SGFja2VyU2VjcmV0S2V5Rm9yVG1kdFByb2plY3RXaGljaElzVmVyeUxvbmc=";
        ReflectionTestUtils.setField(hackerJwtService, "secretKey", HACKER_SECRET);
        ReflectionTestUtils.setField(hackerJwtService, "jwtExpiration", 3600000L);

        // Hacker tự sinh ra token với quyền admin
        String forgedToken = hackerJwtService.generateToken(email);

        // Đưa token giả mạo này vào hệ thống thật của chúng ta để kiểm tra
        assertThrows(SignatureException.class, () -> {
            jwtService.extractUserName(forgedToken);
        });
    }

    @Test
    @DisplayName("Bảo mật: Báo lỗi MalformedJwtException khi cấu trúc Token bị phá hỏng")
    void validateToken_Fail_WhenTokenIsMalformed() {
        String email = "user@tmdt.com";
        String token = jwtService.generateToken(email);

        // Khách hàng copy thiếu token, hoặc hacker cố tình sửa thêm chữ 'A' vào đuôi
        String malformedToken = token + "A";

        // Hệ thống sẽ bắt lỗi chuỗi không đúng chuẩn JWT ngay lập tức
        assertThrows(MalformedJwtException.class, () -> {
            jwtService.extractUserName(malformedToken);
        });
    }

    // ==========================================
    // 3. KIỂM THỬ REFRESH TOKEN
    // ==========================================

    @Test
    @DisplayName("Refresh Token: Sinh ra thành công với thời gian sống dài hơn")
    void generateRefreshToken_Success() {
        String email = "user@tmdt.com";
        String refreshToken = jwtService.generateRefreshToken(email);

        assertNotNull(refreshToken);
        assertEquals(email, jwtService.extractUserName(refreshToken));

        // Ta không thể test trực tiếp được số ngày hết hạn qua hàm extract (bởi vì nó là private)
        // Nhưng việc extract không sinh ra lỗi ExpiredJwtException nghĩa là nó đã được tạo hợp lệ.
    }
}