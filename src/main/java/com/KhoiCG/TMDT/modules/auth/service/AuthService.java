package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.auth.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.UserRepo;
import com.KhoiCG.TMDT.modules.email.dto.UserCreatedEvent; // Đảm bảo class này tồn tại hoặc dùng Shared Library
import com.KhoiCG.TMDT.modules.auth.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<UserResponse> getAllUsers() {
        // 1. Lấy toàn bộ Entity từ DB
        List<User> users = userRepo.findAll();

        // 2. Map từ Entity sang UserResponse DTO
        return users.stream().map(user -> UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                // .avatar(user.getAvatar()) // Mở comment nếu có
                .build()
        ).toList();
    }

    public UserResponse updateProfile(UserUpdateRequest request) {
        // 1. Lấy email user hiện tại từ Token
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Tìm User
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Cập nhật thông tin (Chỉ update nếu có gửi lên)
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }

        // Nếu bạn muốn update Avatar (Lưu URL ảnh)
        if (request.getAvatar() != null) {
            // user.setAvatar(request.getAvatar());
            // ⚠️ Lưu ý: Bạn cần thêm trường avatar vào Entity User nếu chưa có
        }

        // 4. Lưu vào DB
        User updatedUser = userRepo.save(user);

        // 5. Trả về DTO mới
        return UserResponse.builder()
                .id(String.valueOf(updatedUser.getId()))
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                // .avatar(updatedUser.getAvatar())
                .build();
    }

    public void deleteUser(Long id) {
        // 1. Kiểm tra xem user có tồn tại không
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }

        // 2. (Tùy chọn) Chặn không cho xóa chính mình (nếu cần)
        // String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        // User user = userRepo.findById(id).get();
        // if (user.getEmail().equals(currentEmail)) {
        //     throw new RuntimeException("You cannot delete your own account!");
        // }

        // 3. Xóa user
        userRepo.deleteById(id);
    }
}