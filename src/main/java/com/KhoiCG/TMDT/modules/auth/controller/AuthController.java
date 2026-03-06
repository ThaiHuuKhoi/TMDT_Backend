package com.KhoiCG.TMDT.modules.auth.controller;

import com.KhoiCG.TMDT.modules.auth.service.AuthService;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import com.KhoiCG.TMDT.modules.auth.dto.*;
import com.KhoiCG.TMDT.modules.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;

    private ResponseCookie createSecureCookie(String refreshToken) {
        long maxAgeInSeconds = jwtService.getRefreshExpiration() / 1000;

        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeInSeconds)
                .sameSite("Strict")
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);

        ResponseCookie cookie = createSecureCookie(authResponse.getRefreshToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse authResponse = authService.login(request);

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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(email);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0)
                .sameSite("Strict").build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        try {
            passwordResetService.processForgotPassword(request.getEmail());
        } catch (Exception e) {
            // Best practice: Luôn trả về thông báo thành công chung chung để chống dò rỉ email (User Enumeration)
            // Dù email có sai hay đúng thì hacker cũng không biết được.
        }
        return ResponseEntity.ok("Nếu email của bạn tồn tại trong hệ thống, hướng dẫn khôi phục mật khẩu sẽ được gửi đến hộp thư.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập lại với mật khẩu mới.");
    }
}