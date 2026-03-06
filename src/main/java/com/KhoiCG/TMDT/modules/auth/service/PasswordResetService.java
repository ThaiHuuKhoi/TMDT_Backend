package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.entity.PasswordResetToken;
import com.KhoiCG.TMDT.modules.auth.event.PasswordResetRequestedEvent;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.PasswordResetTokenRepository;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30)) // Token sống 30 phút
                .isUsed(false)
                .build();

        tokenRepository.save(resetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, token));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc không tồn tại"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu lại.");
        }

        if (resetToken.getIsUsed()) {
            throw new RuntimeException("Token này đã được sử dụng.");
        }

        User user = resetToken.getUser();

        user.getProviders().stream()
                .filter(p -> p.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .ifPresentOrElse(
                        provider -> provider.setPasswordHash(passwordEncoder.encode(newPassword)),
                        () -> { throw new RuntimeException("Tài khoản này không đăng nhập bằng email/mật khẩu thông thường"); }
                );

        userRepo.save(user);

        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);
    }
}