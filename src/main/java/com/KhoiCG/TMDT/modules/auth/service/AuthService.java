package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent;
import com.KhoiCG.TMDT.modules.auth.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 1. Tạo thông tin User
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .role("USER")
                .build();

        // 2. Tạo thông tin Provider (Mật khẩu)
        UserProvider localProvider = UserProvider.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        // 3. Liên kết Provider vào User
        user.getProviders().add(localProvider);

        // 4. Lưu vào Database (sẽ insert cả 2 bảng)
        User savedUser = userRepo.save(user);

        try {
            UserCreatedEvent event = new UserCreatedEvent();
            event.setEmail(savedUser.getEmail());
            event.setUsername(savedUser.getName());
            kafkaTemplate.send("user.created", event);
        } catch (Exception e) {
            log.error("Error sending Kafka event: {}", e.getMessage());
        }

        String accessToken = jwtService.generateToken(savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow();

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        tokenService.saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse processRefreshToken(String oldRefreshToken) {
        // Logic rotation đã được tách sang TokenService
        return tokenService.rotateRefreshToken(oldRefreshToken);
    }

    public void logout(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        tokenService.deleteTokensByUser(user);
    }

    public String getLatestRefreshToken(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return tokenService.getLatestRefreshToken(user);
    }
}
