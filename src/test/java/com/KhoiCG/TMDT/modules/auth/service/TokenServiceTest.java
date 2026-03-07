package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.dto.AuthResponse;
import com.KhoiCG.TMDT.modules.auth.entity.RefreshToken;
import com.KhoiCG.TMDT.modules.auth.repository.RefreshTokenRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenService tokenService;

    private User mockUser;
    private RefreshToken validRefreshToken;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@tmdt.com")
                .build();

        validRefreshToken = RefreshToken.builder()
                .id(100L)
                .user(mockUser)
                .token("old-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    // ==========================================
    // 1. TEST LƯU TOKEN
    // ==========================================

    @Test
    @DisplayName("Lưu Token: Tính toán đúng thời gian hết hạn và lưu vào DB")
    void saveRefreshToken_Success() {
        // Giả lập thời gian sống của Refresh Token là 7 ngày (604,800,000 milliseconds)
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L);

        tokenService.saveRefreshToken(mockUser, "new-refresh-token");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals("new-refresh-token", savedToken.getToken());
        assertEquals(mockUser, savedToken.getUser());
        assertFalse(savedToken.isRevoked()); // Token mới lưu không được phép bị thu hồi
        assertNotNull(savedToken.getExpiryDate());
    }

    // ==========================================
    // 2. TEST XOAY VÒNG TOKEN (ROTATION)
    // ==========================================

    @Test
    @DisplayName("Xoay vòng Token: Thành công - Xóa token cũ, tạo và lưu token mới")
    void rotateRefreshToken_Success() {
        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(validRefreshToken));

        // Mock việc tạo token mới
        when(jwtService.generateToken("test@tmdt.com")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken("test@tmdt.com")).thenReturn("new-refresh-token");
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L); // Mock cho hàm saveRefreshToken bên trong

        AuthResponse response = tokenService.rotateRefreshToken("old-refresh-token");

        // 1. Kiểm tra kết quả trả về có đúng cặp token mới không
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        // 2. BẮT BUỘC: Phải xóa token cũ đi để chống dùng lại
        verify(refreshTokenRepository, times(1)).delete(validRefreshToken);

        // 3. Phải gọi hàm lưu token mới
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Xoay vòng Token: Báo lỗi nếu token gửi lên không tồn tại trong DB")
    void rotateRefreshToken_Fail_NotFound() {
        when(refreshTokenRepository.findByToken("fake-token")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            tokenService.rotateRefreshToken("fake-token");
        });

        assertEquals("Refresh token không tồn tại!", ex.getMessage());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xoay vòng Token: Báo lỗi và xóa luôn token nếu nó đã hết hạn hoặc bị thu hồi")
    void rotateRefreshToken_Fail_ExpiredOrRevoked() {
        // Cố tình làm cho token bị hết hạn
        validRefreshToken.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(validRefreshToken));

        Exception ex = assertThrows(RuntimeException.class, () -> {
            tokenService.rotateRefreshToken("old-refresh-token");
        });

        assertEquals("Token đã hết hạn hoặc bị vô hiệu", ex.getMessage());

        // Cực kỳ quan trọng: Hệ thống phát hiện token rác thì phải tự động dọn (xóa) nó đi luôn
        verify(refreshTokenRepository, times(1)).delete(validRefreshToken);

        // Không được phép sinh token mới
        verify(jwtService, never()).generateToken(anyString());
    }

    // ==========================================
    // 3. TEST CÁC HÀM TIỆN ÍCH KHÁC
    // ==========================================

    @Test
    @DisplayName("Lấy Token mới nhất: Trả về string token nếu tìm thấy")
    void getLatestRefreshToken_Success() {
        when(refreshTokenRepository.findFirstByUserOrderByExpiryDateDesc(mockUser))
                .thenReturn(Optional.of(validRefreshToken));

        String result = tokenService.getLatestRefreshToken(mockUser);

        assertEquals("old-refresh-token", result);
    }

    @Test
    @DisplayName("Lấy Token mới nhất: Ném lỗi nếu User chưa từng có token nào")
    void getLatestRefreshToken_Fail_NotFound() {
        when(refreshTokenRepository.findFirstByUserOrderByExpiryDateDesc(mockUser))
                .thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            tokenService.getLatestRefreshToken(mockUser);
        });

        assertEquals("No refresh token found", ex.getMessage());
    }

    @Test
    @DisplayName("Xóa Tokens theo User: Gọi đúng lệnh xóa của Repository (Dùng cho đăng xuất)")
    void deleteTokensByUser_Success() {
        tokenService.deleteTokensByUser(mockUser);
        verify(refreshTokenRepository, times(1)).deleteByUser(mockUser);
    }

    @Test
    @DisplayName("Dọn rác tự động (Cronjob): Gọi lệnh xóa các token có expiryDate < Now")
    void purgeExpiredTokens_Success() {
        tokenService.purgeExpiredTokens();
        verify(refreshTokenRepository, times(1)).deleteByExpiryDateBefore(any(LocalDateTime.class));
    }
}