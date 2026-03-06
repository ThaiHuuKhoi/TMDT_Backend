package com.KhoiCG.TMDT.modules.user.service;

import com.KhoiCG.TMDT.modules.user.dto.UserResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserUpdateRequest;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::mapToResponse).toList();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepo.save(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UserUpdateRequest request) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        return mapToResponse(userRepo.save(user));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .isActive(user.getIsActive())
                .build();
    }
}