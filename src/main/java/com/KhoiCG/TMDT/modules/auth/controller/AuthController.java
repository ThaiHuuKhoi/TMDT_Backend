package com.KhoiCG.TMDT.modules.auth.controller;

import com.KhoiCG.TMDT.modules.auth.service.AuthService;
import com.KhoiCG.TMDT.modules.auth.service.AuthLoginRateLimiter;
import com.KhoiCG.TMDT.modules.auth.service.AuthRegistrationRateLimiter;
import com.KhoiCG.TMDT.modules.auth.service.FeatureFlagService;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import com.KhoiCG.TMDT.modules.auth.dto.*;
import com.KhoiCG.TMDT.modules.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;
    private final AuthRegistrationRateLimiter authRegistrationRateLimiter;
    private final AuthLoginRateLimiter authLoginRateLimiter;
    private final FeatureFlagService featureFlagService;

    @Value("${application.security.cookies.secure:true}")
    private boolean secureCookies;

    @Value("${application.security.cookies.refresh.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${application.security.trust-forward-headers:false}")
    private boolean trustForwardHeaders;

    private ResponseCookie createSecureCookie(String refreshToken) {
        long maxAgeInSeconds = jwtService.getRefreshExpiration() / 1000;

        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .maxAge(maxAgeInSeconds)
                .sameSite(refreshCookieSameSite)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        if (featureFlagService.isOtpRegistrationEnabled()) {
            authService.register(request, clientIp(httpRequest));
            return ResponseEntity.ok(RegisterResponse.builder()
                    .requiresOtp(true)
                    .message("OTP đã được gửi tới email của bạn. Vui lòng xác thực để hoàn tất đăng ký.")
                    .build());
        } else {
            AuthResponse authResponse = authService.registerDirect(request, clientIp(httpRequest));
            ResponseCookie cookie = createSecureCookie(authResponse.getRefreshToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(RegisterResponse.builder()
                            .requiresOtp(false)
                            .accessToken(authResponse.getAccessToken())
                            .build());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request, HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.verifyRegistrationOtp(request, clientIp(httpRequest));
        ResponseCookie cookie = createSecureCookie(authResponse.getRefreshToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        authLoginRateLimiter.assertLoginAllowed(request.getEmail(), clientIp(httpRequest));
        AuthResponse authResponse;
        try {
            authResponse = authService.login(request);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            authLoginRateLimiter.onLoginFailure(request.getEmail());
            throw ex;
        }
        authLoginRateLimiter.onLoginSuccess(request.getEmail());

        ResponseCookie cookie = createSecureCookie(authResponse.getRefreshToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(authResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = true) String oldRefreshToken) {

        AuthResponse authResponse = authService.processRefreshToken(oldRefreshToken);

        ResponseCookie newCookie = createSecureCookie(authResponse.getRefreshToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, newCookie.toString()).body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String email = authentication.getName();
        authService.logout(email);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(secureCookies).path("/")
                .maxAge(0)
                .sameSite(refreshCookieSameSite).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authRegistrationRateLimiter.assertForgotPasswordAllowed(request.getEmail(), clientIp(httpRequest));
        try {
            passwordResetService.processForgotPassword(request.getEmail());
        } catch (Exception e) {
            log.warn("forgot-password failed for email={} cause={}", request.getEmail(), e.getMessage());
        }
        return ResponseEntity.ok("Nếu email của bạn tồn tại trong hệ thống, hướng dẫn khôi phục mật khẩu sẽ được gửi đến hộp thư.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request, HttpServletRequest httpRequest) {
        authRegistrationRateLimiter.assertResetPasswordAllowed(clientIp(httpRequest));
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập lại với mật khẩu mới.");
    }

    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        authRegistrationRateLimiter.assertChangePasswordAllowed(authentication.getName(), clientIp(httpRequest));
        authService.changePassword(authentication.getName(), request.getOldPassword(), request.getNewPassword());
        ResponseCookie clearedCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(secureCookies).path("/")
                .maxAge(0).sameSite(refreshCookieSameSite).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .body("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "";
    }
}