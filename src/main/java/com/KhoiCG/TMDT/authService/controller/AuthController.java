package com.KhoiCG.TMDT.authService.controller;

import com.KhoiCG.TMDT.authService.dto.AuthResponse;
import com.KhoiCG.TMDT.authService.dto.LoginRequest;
import com.KhoiCG.TMDT.authService.dto.RegisterRequest;
import com.KhoiCG.TMDT.authService.repository.UserRepo;
import com.KhoiCG.TMDT.authService.service.AuthService;
import com.KhoiCG.TMDT.authService.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // Prefix API
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {


        // 1. Xác thực qua Spring Security (Kiểm tra DB, so khớp BCrypt)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Nếu xác thực thành công, code sẽ chạy xuống đây -> Tạo Token
        // authentication.getName() sẽ trả về email (do UserPrincipal config)
        String token = jwtService.generateToken(authentication.getName());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // 1. Lấy email từ SecurityContext (Do JwtFilter đã set)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        // 2. Tìm user trong DB
        return userRepo.findByEmail(email)
                .map(user -> {
                    // Trả về thông tin cần thiết (tránh trả về password)
                    user.setPassword(null);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Bạn nên làm thêm API /register để user tạo mật khẩu
}
