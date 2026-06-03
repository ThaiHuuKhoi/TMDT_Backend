package com.KhoiCG.TMDT.modules.user.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.user.dto.UserPageResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserRoleUpdateRequest;
import com.KhoiCG.TMDT.modules.user.dto.UserStatusUpdateRequest;
import com.KhoiCG.TMDT.modules.user.dto.UserUpdateRequest;
import com.KhoiCG.TMDT.modules.user.entity.Role;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepo userRepo;

    public UserPageResponse getAllUsers(int page, int size, String q, Boolean isActive) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String keyword = q == null ? "" : q.trim();
        Page<User> pageResult;
        if (keyword.isEmpty()) {
            pageResult = (isActive == null
                    ? userRepo.findAll(pageable)
                    : userRepo.findByIsActiveAndEmailContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCase(
                            isActive, "", isActive, "", pageable));
        } else if (isActive == null) {
            pageResult = userRepo.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            pageResult = userRepo.findByIsActiveAndEmailContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCase(
                    isActive, keyword, isActive, keyword, pageable
            );
        }
        Page<UserResponse> mapped = pageResult.map(this::mapToResponse);
        return UserPageResponse.builder()
                .items(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalItems(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .hasNext(mapped.hasNext())
                .hasPrevious(mapped.hasPrevious())
                .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng."));
        return mapToResponse(user);
    }

    public UserResponse getUserByIdForAdmin(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng."));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UserUpdateRequest request) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng."));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "Tên không được để trống.");
            }
            user.setName(trimmedName);
        }
        if (request.getAvatar() != null) {
            String trimmedAvatar = request.getAvatar().trim();
            user.setAvatar(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
        }

        return mapToResponse(userRepo.save(user));
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatusUpdateRequest request, String actorEmail) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng."));
        boolean beforeActive = Boolean.TRUE.equals(user.getIsActive());
        boolean afterActive = Boolean.TRUE.equals(request.getIsActive());
        if (!afterActive && user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_DEACTIVATION_FORBIDDEN", "Bạn không thể tự vô hiệu hóa tài khoản của mình.");
        }
        user.setIsActive(request.getIsActive());
        User saved = userRepo.save(user);
        log.info(
                "admin_user_status_change actor={} targetId={} targetEmail={} beforeActive={} afterActive={}",
                actorEmail,
                saved.getId(),
                saved.getEmail(),
                beforeActive,
                afterActive
        );
        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UserRoleUpdateRequest request, String actorEmail) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng."));
        if (user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_ROLE_CHANGE_FORBIDDEN", "Bạn không thể tự thay đổi quyền của mình.");
        }
        Role before = user.getRole();
        user.setRole(request.getRole());
        User saved = userRepo.save(user);
        log.info("admin_user_role_change actor={} targetId={} targetEmail={} before={} after={}",
                actorEmail, saved.getId(), saved.getEmail(), before, request.getRole());
        return mapToResponse(saved);
    }

    private UserResponse mapToResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .avatar(user.getAvatar())
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}
