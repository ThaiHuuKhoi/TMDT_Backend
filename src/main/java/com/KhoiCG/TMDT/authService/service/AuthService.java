package com.KhoiCG.TMDT.authService.service;

import com.KhoiCG.TMDT.authService.dto.RegisterRequest;
import com.KhoiCG.TMDT.authService.entity.AuthProvider;
import com.KhoiCG.TMDT.authService.entity.User;
import com.KhoiCG.TMDT.authService.repository.UserRepo;
import com.KhoiCG.TMDT.emailService.dto.UserCreatedEvent; // Tận dụng DTO bên Email
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public User register(RegisterRequest request) {
        // 1. Kiểm tra email trùng
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Tạo User mới
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 👇 Set Role mặc định là USER
        user.setRole("USER");

        // 👇 Set Provider là LOCAL (dùng Enum vừa tạo)
        user.setProvider(AuthProvider.LOCAL);

        // ProviderId của Local có thể để null hoặc chính là email
        user.setProviderId(null);

        User savedUser = userRepository.save(user);

        // 3. Gửi sự kiện Kafka (Giữ nguyên code cũ)
        UserCreatedEvent event = new UserCreatedEvent();
        event.setEmail(savedUser.getEmail());
        event.setUsername(savedUser.getName());

        // Lưu ý: Nếu Kafka Producer config chưa có JsonSerializer thì dùng ObjectMapper convert sang String như bài trước nhé
        kafkaTemplate.send("user.created", event);

        return savedUser;
    }
}