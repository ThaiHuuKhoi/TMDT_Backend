package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.auth.entity.PasswordResetToken;
import com.KhoiCG.TMDT.modules.auth.event.PasswordResetRequestedEvent;
import com.KhoiCG.TMDT.modules.auth.util.RefreshTokenHasher;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.PasswordResetTokenRepository;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenService tokenService;

    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "EMAIL_NOT_FOUND",
                        "Email không tồn tại trong hệ thống"
                ));

        tokenRepository.deleteByUser(user);

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = RefreshTokenHasher.sha256Hex(rawToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .isUsed(false)
                .build();

        tokenRepository.save(resetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, rawToken));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hashedToken = RefreshTokenHasher.sha256Hex(token);
        PasswordResetToken resetToken = tokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESET_TOKEN",
                        "Token không hợp lệ hoặc không tồn tại"
                ));

        if (resetToken.isExpired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EXPIRED_RESET_TOKEN", "Token đã hết hạn. Vui lòng yêu cầu lại.");
        }

        if (resetToken.getIsUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USED_RESET_TOKEN", "Token này đã được sử dụng.");
        }

        User user = resetToken.getUser();

        user.getProviders().stream()
                .filter(p -> p.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .ifPresentOrElse(
                        provider -> provider.setPasswordHash(passwordEncoder.encode(newPassword)),
                        () -> {
                            throw new ApiException(
                                    HttpStatus.BAD_REQUEST,
                                    "NON_LOCAL_ACCOUNT",
                                    "Tài khoản này không đăng nhập bằng email/mật khẩu thông thường"
                            );
                        }
                );

        userRepo.save(user);

        // Vô hiệu hóa tất cả session đang hoạt động sau khi đổi mật khẩu
        tokenService.deleteTokensByUser(user);

        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);
    }
}