package com.KhoiCG.TMDT.modules.auth.controller;

import com.KhoiCG.TMDT.modules.auth.service.AuthService;
import com.KhoiCG.TMDT.modules.auth.dto.*;
import jakarta.servlet.http.HttpServletResponse;
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

    // Helper method tạo Cookie an toàn
    private ResponseCookie createSecureCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
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
                .maxAge(0) // 0 = Xóa ngay lập tức
                .sameSite("Strict").build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }
}