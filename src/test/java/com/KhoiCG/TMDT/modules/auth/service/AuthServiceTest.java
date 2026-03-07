package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.dto.AuthResponse;
import com.KhoiCG.TMDT.modules.auth.dto.LoginRequest;
import com.KhoiCG.TMDT.modules.auth.dto.RegisterRequest;
import com.KhoiCG.TMDT.modules.auth.event.UserRegisteredEvent;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Đăng ký: Thành công, lưu User, tạo Token và phát Event")
    void register_Success() {
        // Arrange
        when(userRepo.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepo.save(any(User.class))).thenReturn(mockUser);

        when(jwtService.generateToken(mockUser.getEmail())).thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(mockUser.getEmail())).thenReturn("mockRefreshToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());

        // Kiểm tra xem mật khẩu đã được băm trước khi lưu chưa
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertEquals("hashedPassword", userCaptor.getValue().getProviders().get(0).getPasswordHash());

        // Kiểm tra xem hệ thống đã phát Event chào mừng chưa
        verify(eventPublisher, times(1)).publishEvent(any(UserRegisteredEvent.class));

        // Kiểm tra xem refresh token đã được lưu vào DB để quản lý đăng xuất chưa
        verify(tokenService, times(1)).saveRefreshToken(mockUser, "mockRefreshToken");
    }

    @Test
    @DisplayName("Đăng ký: Thất bại, ném lỗi khi Email đã tồn tại trong hệ thống")
    void register_Fail_EmailExists() {
        // Arrange: Giả sử email đã bị người khác đăng ký
        when(userRepo.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Email đã được sử dụng!", ex.getMessage());

        // Tuyệt đối không được gọi hàm save hay generateToken nếu bị lỗi chặn lại
        verify(userRepo, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(anyString());
        verify(eventPublisher, never()).publishEvent(any());
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
        when(jwtService.generateToken(mockUser.getEmail())).thenReturn("newAccessToken");
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