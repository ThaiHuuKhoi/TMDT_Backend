package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.auth.event.UserRegisteredEvent;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.Role;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.KhoiCG.TMDT.modules.auth.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.KhoiCG.TMDT.modules.email.service.NotificationService;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthRegistrationRateLimiter authRegistrationRateLimiter;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final String REGISTRATION_OTP_PREFIX = "auth:reg-otp:";

    @Value("${application.security.registration-otp.expiration-minutes:10}")
    private int registrationOtpExpiryMinutes;

    public void register(RegisterRequest request, String clientIp) {
        authRegistrationRateLimiter.assertRegisterAllowed(request.getEmail(), clientIp);
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email đã được sử dụng.");
        }

        String otpCode = generateOtpCode();
        RegistrationOtpCachePayload payload = new RegistrationOtpCachePayload(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                passwordEncoder.encode(otpCode)
        );
        try {
            stringRedisTemplate.opsForValue().set(
                    getRegistrationOtpKey(request.getEmail()),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofMinutes(registrationOtpExpiryMinutes)
            );
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OTP_CREATE_FAILED", "Không thể tạo OTP đăng ký.");
        }
        try {
            notificationService.sendRegistrationOtpEmail(request.getEmail(), request.getName(), otpCode);
        } catch (MessagingException e) {
            stringRedisTemplate.delete(getRegistrationOtpKey(request.getEmail()));
            log.error("Không gửi được email OTP cho {}: {}", request.getEmail(), e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OTP_EMAIL_SEND_FAILED", "Không thể gửi email OTP. Vui lòng thử lại sau.");
        }
    }

    @Transactional
    public AuthResponse registerDirect(RegisterRequest request, String clientIp) {
        authRegistrationRateLimiter.assertRegisterAllowed(request.getEmail(), clientIp);
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email đã được sử dụng.");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .role(Role.USER)
                .build();
        UserProvider localProvider = UserProvider.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user.getProviders().add(localProvider);
        User savedUser = userRepo.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest request, String clientIp) {
        authRegistrationRateLimiter.assertVerifyOtpAllowed(clientIp);

        String otpKey = getRegistrationOtpKey(request.getEmail());
        String payloadJson = stringRedisTemplate.opsForValue().get(otpKey);
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "OTP không hợp lệ hoặc đã hết hạn.");
        }
        RegistrationOtpCachePayload payload;
        try {
            payload = objectMapper.readValue(payloadJson, RegistrationOtpCachePayload.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "OTP không hợp lệ hoặc đã hết hạn.");
        }

        if (!passwordEncoder.matches(request.getOtpCode(), payload.getOtpHash())) {
            authRegistrationRateLimiter.onOtpMismatch(request.getEmail());
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "OTP không đúng hoặc đã hết hạn.");
        }
        authRegistrationRateLimiter.clearOtpFailures(request.getEmail());
        if (userRepo.existsByEmail(payload.getEmail())) {
            stringRedisTemplate.delete(otpKey);
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email đã được sử dụng.");
        }

        User user = User.builder()
                .name(payload.getName())
                .email(payload.getEmail())
                .role(Role.USER)
                .build();

        UserProvider localProvider = UserProvider.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .passwordHash(payload.getPasswordHash())
                .build();

        user.getProviders().add(localProvider);

        User savedUser = userRepo.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
        stringRedisTemplate.delete(otpKey);

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tài khoản không tồn tại."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hóa.");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse processRefreshToken(String oldRefreshToken) {
        return tokenService.rotateRefreshToken(oldRefreshToken);
    }

    public void logout(String email) {
        tokenService.deleteTokensByEmail(email);
    }

    @Transactional
    public String processOAuth2Login(String email, String name, AuthProvider provider, String providerId) {
        User user = userRepo.findByEmail(email).orElse(null);

        if (user == null) {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            UserProvider oauthProvider = UserProvider.builder()
                    .user(newUser)
                    .provider(provider)
                    .providerUserId(providerId)
                    .build();
            newUser.getProviders().add(oauthProvider);
            user = userRepo.save(newUser);
        } else {
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hóa.");
            }

            boolean hasProvider = user.getProviders().stream()
                    .anyMatch(p -> p.getProvider() == provider);
            if (!hasProvider) {
                UserProvider newProvider = UserProvider.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(providerId)
                        .build();
                user.getProviders().add(newProvider);
                userRepo.save(user);
            }
        }

        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        tokenService.saveRefreshToken(user, refreshToken);
        return refreshToken;
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepo.findWithProvidersByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại."));

        UserProvider localProvider = user.getProviders().stream()
                .filter(p -> p.getProvider() == AuthProvider.LOCAL)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "NO_LOCAL_ACCOUNT",
                        "Tài khoản này không có mật khẩu. Hãy dùng đăng nhập qua mạng xã hội."));

        if (!passwordEncoder.matches(oldPassword, localProvider.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_OLD_PASSWORD", "Mật khẩu hiện tại không đúng.");
        }

        if (oldPassword.equals(newPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "Mật khẩu mới không được trùng mật khẩu cũ.");
        }

        localProvider.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        tokenService.deleteTokensByUser(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        String accessToken = jwtService.generateToken(user.getEmail(), roleName);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        tokenService.saveRefreshToken(user, refreshToken);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateOtpCode() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    private String getRegistrationOtpKey(String email) {
        return REGISTRATION_OTP_PREFIX + email.trim().toLowerCase();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RegistrationOtpCachePayload {
        private String name;
        private String email;
        private String passwordHash;
        private String otpHash;
    }
}
