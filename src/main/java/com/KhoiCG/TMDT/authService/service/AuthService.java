package com.KhoiCG.TMDT.authService.service;

import com.KhoiCG.TMDT.authService.dto.*;
import com.KhoiCG.TMDT.authService.entity.AuthProvider;
import com.KhoiCG.TMDT.authService.entity.User;
import com.KhoiCG.TMDT.authService.repository.UserRepo;
import com.KhoiCG.TMDT.emailService.dto.UserCreatedEvent; // Đảm bảo class này tồn tại hoặc dùng Shared Library
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ================= REGISTER =================
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra email trùng
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Tạo User mới bằng Builder (Code gọn hơn)
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepo.save(user);

        // 3. Gửi sự kiện Kafka (Bất đồng bộ - Không để lỗi Kafka làm fail luồng đăng ký)
        try {
            UserCreatedEvent event = new UserCreatedEvent();
            event.setEmail(savedUser.getEmail());
            event.setUsername(savedUser.getName());

            // Topic "user.created" nên đưa vào constant hoặc properties
            kafkaTemplate.send("user.created", event);
            log.info("Sent Kafka event for user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Error sending Kafka event: {}", e.getMessage());
            // Không throw exception ở đây để user vẫn đăng ký thành công
        }

        // 4. Auto-Login: Tạo Token ngay để user không phải đăng nhập lại
        String token = jwtService.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    // ================= LOGIN =================
    public AuthResponse login(LoginRequest request) {
        // 1. Xác thực qua AuthenticationManager (Sẽ gọi UserDetailsService)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Nếu pass bước trên -> Tạo Token
        // Tìm lại user để chắc chắn (hoặc lấy từ Principal)
        var user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    // ================= GET CURRENT USER =================
    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatar(null) // Cập nhật sau nếu có trường avatar
                .build();
    }
}