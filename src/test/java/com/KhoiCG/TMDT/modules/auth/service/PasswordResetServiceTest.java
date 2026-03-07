package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.entity.PasswordResetToken;
import com.KhoiCG.TMDT.modules.auth.event.PasswordResetRequestedEvent;
import com.KhoiCG.TMDT.modules.auth.repository.PasswordResetTokenRepository;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepo userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User mockUser;
    private UserProvider localProvider;
    private PasswordResetToken validToken;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .providers(new ArrayList<>())
                .build();

        localProvider = UserProvider.builder()
                .provider(AuthProvider.LOCAL)
                .passwordHash("oldHash")
                .user(mockUser)
                .build();
        mockUser.getProviders().add(localProvider);

        validToken = PasswordResetToken.builder()
                .id(100L)
                .token("valid-uuid-token")
                .user(mockUser)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .isUsed(false)
                .build();
    }

    // ==========================================
    // 1. TEST LUỒNG YÊU CẦU QUÊN MẬT KHẨU
    // ==========================================

    @Test
    @DisplayName("Quên mật khẩu: Thành công - Xóa token cũ, tạo token mới và phát Event gửi mail")
    void processForgotPassword_Success() {
        when(userRepo.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

        passwordResetService.processForgotPassword("test@gmail.com");

        // 1. Phải xóa token cũ
        verify(tokenRepository, times(1)).deleteByUser(mockUser);

        // 2. Phải lưu token mới vào DB
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken()); // UUID đã được tạo
        assertEquals(mockUser, savedToken.getUser());
        assertFalse(savedToken.getIsUsed());

        // 3. Phải phát Event để gửi Email
        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals("test@gmail.com", eventCaptor.getValue().getEmail());
        assertEquals(savedToken.getToken(), eventCaptor.getValue().getToken());
    }

    @Test
    @DisplayName("Quên mật khẩu: Ném lỗi nếu Email không tồn tại")
    void processForgotPassword_Fail_EmailNotFound() {
        when(userRepo.findByEmail("wrong@gmail.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.processForgotPassword("wrong@gmail.com")
        );

        assertEquals("Email không tồn tại trong hệ thống", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ==========================================
    // 2. TEST LUỒNG ĐẶT LẠI MẬT KHẨU
    // ==========================================

    @Test
    @DisplayName("Đặt lại mật khẩu: Thành công - Đổi mật khẩu, mã hóa và khóa Token")
    void resetPassword_Success() {
        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");

        passwordResetService.resetPassword("valid-uuid-token", "newPassword123");

        // 1. Kiểm tra mật khẩu đã được mã hóa và cập nhật vào provider chưa
        assertEquals("newHashedPassword", localProvider.getPasswordHash());
        verify(userRepo, times(1)).save(mockUser);

        // 2. Kiểm tra Token đã bị đánh dấu là "đã sử dụng" chưa (chống dùng lại)
        assertTrue(validToken.getIsUsed());
        verify(tokenRepository, times(1)).save(validToken);
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: Báo lỗi khi Token không tồn tại")
    void resetPassword_Fail_TokenNotFound() {
        when(tokenRepository.findByToken("fake-token")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetPassword("fake-token", "newPass")
        );

        assertEquals("Token không hợp lệ hoặc không tồn tại", ex.getMessage());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: Báo lỗi khi Token đã hết hạn (> 30 phút)")
    void resetPassword_Fail_TokenExpired() {
        validToken.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // Cố tình set hết hạn
        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));

        Exception ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetPassword("valid-uuid-token", "newPass")
        );

        assertEquals("Token đã hết hạn. Vui lòng yêu cầu lại.", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: Báo lỗi khi Token đã bị sử dụng trước đó")
    void resetPassword_Fail_TokenAlreadyUsed() {
        validToken.setIsUsed(true); // Cố tình set đã sử dụng
        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));

        Exception ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetPassword("valid-uuid-token", "newPass")
        );

        assertEquals("Token này đã được sử dụng.", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: Báo lỗi nếu tài khoản chỉ đăng nhập bằng Google/Github (Không có LOCAL provider)")
    void resetPassword_Fail_NoLocalProvider() {
        // Tẩy xóa Provider LOCAL, thay bằng GOOGLE
        mockUser.getProviders().clear();
        mockUser.getProviders().add(UserProvider.builder().provider(AuthProvider.GOOGLE).build());

        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));

        Exception ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetPassword("valid-uuid-token", "newPass")
        );

        assertEquals("Tài khoản này không đăng nhập bằng email/mật khẩu thông thường", ex.getMessage());
        verify(userRepo, never()).save(any());
    }
}