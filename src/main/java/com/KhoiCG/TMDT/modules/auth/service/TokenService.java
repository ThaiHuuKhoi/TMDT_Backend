package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.dto.AuthResponse;
import com.KhoiCG.TMDT.modules.auth.entity.RefreshToken;
import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.auth.util.RefreshTokenHasher;
import com.KhoiCG.TMDT.modules.user.entity.Role;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public void saveRefreshToken(User user, String plainRefreshToken) {
        // Dọn token hết hạn của user trước khi thêm mới để tránh tích lũy vô hạn
        refreshTokenRepository.deleteByUserAndExpiryDateBefore(user, LocalDateTime.now());
        RefreshToken refreshToken = RefreshToken.builder().user(user).build();
        refreshToken.setToken(RefreshTokenHasher.sha256Hex(plainRefreshToken));
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse rotateRefreshToken(String oldPlainRefreshToken) {
        String oldHash = RefreshTokenHasher.sha256Hex(oldPlainRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByToken(oldHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ."));

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh token đã hết hạn hoặc bị vô hiệu.");
        }

        User user = refreshToken.getUser();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hóa.");
        }

        refreshTokenRepository.delete(refreshToken);

        String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        String newAccess = jwtService.generateToken(user.getEmail(), roleName);
        String newRefresh = jwtService.generateRefreshToken(user.getEmail());
        saveRefreshToken(user, newRefresh);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();
    }

    @Transactional
    public void deleteTokensByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void deleteTokensByEmail(String email) {
        refreshTokenRepository.deleteByUserEmail(email);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Bắt đầu dọn refresh token hết hạn toàn hệ thống");
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.info("Hoàn thành dọn refresh token hết hạn");
    }
}