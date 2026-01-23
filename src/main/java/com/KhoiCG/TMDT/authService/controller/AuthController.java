package com.KhoiCG.TMDT.authService.controller;

import com.KhoiCG.TMDT.authService.dto.AuthResponse;
import com.KhoiCG.TMDT.authService.dto.LoginRequest;
import com.KhoiCG.TMDT.authService.dto.RegisterRequest;
import com.KhoiCG.TMDT.authService.dto.UserResponse;
import com.KhoiCG.TMDT.authService.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. Đăng ký
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        // Service sẽ trả về Token luôn sau khi đăng ký thành công (Auto-login)
        return ResponseEntity.ok(authService.register(request));
    }

    // 2. Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        // Logic authenticate và generate token chuyển hết vào Service
        return ResponseEntity.ok(authService.login(request));
    }

    // 3. Lấy thông tin cá nhân
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        // Service tự lấy User từ SecurityContext
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}