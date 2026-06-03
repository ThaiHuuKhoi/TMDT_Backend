package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.auth.dto.AuthResponse;
import com.KhoiCG.TMDT.modules.auth.dto.LoginRequest;
import com.KhoiCG.TMDT.modules.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KhoiCG.TMDT.modules.email.service.NotificationService;
import jakarta.mail.MessagingException;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationService notificationService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private AuthRegistrationRateLimiter authRegistrationRateLimiter;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(authRegistrationRateLimiter).assertRegisterAllowed(anyString(), anyString());
        doNothing().when(authRegistrationRateLimiter).assertVerifyOtpAllowed(anyString());
        doNothing().when(authRegistrationRateLimiter).onOtpMismatch(anyString());
        doNothing().when(authRegistrationRateLimiter).clearOtpFailures(anyString());
        mockUser = User.builder()
                .id(1L)
                .email("test@tmdt.com")
                .name("Khoi CG")
                .role("USER")
                .build();

        registerRequest = RegisterRequest.builder()
                .name("Khoi CG")
                .email("test@tmdt.com")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@tmdt.com")
                .password("password123")
                .build();
    }

    // ==========================================
    // 1. TEST LUỒNG ĐĂNG KÝ (REGISTER)
    // ==========================================

    @Test
    @DisplayName("Đăng ký: Thành công, tạo OTP và gửi email xác minh")
    void register_Success() throws Exception {
        when(userRepo.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        authService.register(registerRequest, "127.0.0.1");

        verify(authRegistrationRateLimiter).assertRegisterAllowed(eq(registerRequest.getEmail()), eq("127.0.0.1"));
        InOrder order = inOrder(notificationService, valueOperations);
        order.verify(notificationService)
                .sendRegistrationOtpEmail(eq(registerRequest.getEmail()), eq(registerRequest.getName()), anyString());
        order.verify(valueOperations).set(contains("auth:reg-otp:"), anyString(), any(Duration.class));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký: Thất bại, ném lỗi khi Email đã tồn tại trong hệ thống")
    void register_Fail_EmailExists() throws MessagingException {
        // Arrange: Giả sử email đã bị người khác đăng ký
        when(userRepo.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest, "127.0.0.1"));
        assertEquals("EMAIL_ALREADY_USED", ex.getCode());
        assertEquals("Email đã được sử dụng.", ex.getMessage());

        verify(authRegistrationRateLimiter).assertRegisterAllowed(eq(registerRequest.getEmail()), eq("127.0.0.1"));
        verify(userRepo, never()).save(any(User.class));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        verify(notificationService, never()).sendRegistrationOtpEmail(anyString(), anyString(), anyString());
    }

    // ==========================================
    // 2. TEST LUỒNG ĐĂNG NHẬP (LOGIN)
    // ==========================================

    @Test
    @DisplayName("Đăng nhập: Thành công, xác thực Spring Security và trả về Token mới")
    void login_Success() {
        // Arrange
        // (AuthenticationManager sẽ im lặng cho qua nếu đăng nhập đúng, nếu sai nó sẽ ném BadCredentialsException)
        when(userRepo.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser.getEmail(), mockUser.getRole())).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(mockUser.getEmail())).thenReturn("newRefreshToken");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());

        // Đảm bảo Spring Security đã được gọi để check password
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Kiểm tra xem phiên đăng nhập mới đã được lưu chưa
        verify(tokenService, times(1)).saveRefreshToken(mockUser, "newRefreshToken");
    }

    // ==========================================
    // 3. TEST CÁC LUỒNG PHỤ (LOGOUT & REFRESH)
    // ==========================================

    @Test
    @DisplayName("Đăng xuất: Xóa toàn bộ Token của User khỏi hệ thống")
    void logout_Success() {
        // Arrange
        when(userRepo.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));

        // Act
        authService.logout(mockUser.getEmail());

        // Assert
        verify(tokenService, times(1)).deleteTokensByUser(mockUser);
    }

    @Test
    @DisplayName("Làm mới Token: Gọi đúng logic xoay vòng (Rotation) của TokenService")
    void processRefreshToken_Success() {
        // Arrange
        String oldToken = "old_refresh_token_string";
        AuthResponse newMockResponse = AuthResponse.builder().accessToken("newA").refreshToken("newR").build();

        when(tokenService.rotateRefreshToken(oldToken)).thenReturn(newMockResponse);

        // Act
        AuthResponse result = authService.processRefreshToken(oldToken);

        // Assert
        assertNotNull(result);
        assertEquals("newA", result.getAccessToken());
        verify(tokenService, times(1)).rotateRefreshToken(oldToken);
    }
}