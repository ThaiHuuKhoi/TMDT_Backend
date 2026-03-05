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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        String refreshToken = authService.getLatestRefreshToken(request.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true).secure(true).path("/").maxAge(7 * 24 * 60 * 60).sameSite("Strict").build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(email);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0).path("/").build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody @Valid TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.processRefreshToken(request.getRefreshToken()));
    }
}