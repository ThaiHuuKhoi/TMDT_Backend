package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.dto.AuthResponse;
import com.KhoiCG.TMDT.modules.auth.entity.RefreshToken;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse rotateRefreshToken(String oldRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại!"));

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Token đã hết hạn hoặc bị vô hiệu");
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);

        String newAccess = jwtService.generateToken(user.getEmail());
        String newRefresh = jwtService.generateRefreshToken(user.getEmail());
        saveRefreshToken(user, newRefresh);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();
    }

    public String getLatestRefreshToken(User user) {
        return refreshTokenRepository.findFirstByUserOrderByExpiryDateDesc(user)
                .map(RefreshToken::getToken)
                .orElseThrow(() -> new RuntimeException("No refresh token found"));
    }

    @Transactional
    public void deleteTokensByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}