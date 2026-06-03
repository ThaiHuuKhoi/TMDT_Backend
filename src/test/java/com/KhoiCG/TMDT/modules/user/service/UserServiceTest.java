package com.KhoiCG.TMDT.modules.user.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.user.dto.UserPageResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserStatusUpdateRequest;
import com.KhoiCG.TMDT.modules.user.dto.UserUpdateRequest;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("user@tmdt.com")
                .name("User A")
                .role("USER")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách users có phân trang")
    void getAllUsers_WithPagination() {
        Page<User> usersPage = new PageImpl<>(List.of(mockUser));
        when(userRepo.findAll(any(Pageable.class))).thenReturn(usersPage);

        UserPageResponse responsePage = userService.getAllUsers(0, 20, null, null);

        assertEquals(1, responsePage.getTotalItems());
        verify(userRepo).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Lấy danh sách users: search theo từ khóa")
    void getAllUsers_WithSearchKeyword() {
        Page<User> usersPage = new PageImpl<>(List.of(mockUser));
        when(userRepo.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(any(), any(), any(Pageable.class)))
                .thenReturn(usersPage);

        UserPageResponse responsePage = userService.getAllUsers(0, 20, "user@", null);

        assertEquals(1, responsePage.getTotalItems());
        verify(userRepo).findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Lấy danh sách users: filter theo trạng thái active")
    void getAllUsers_WithActiveFilter() {
        Page<User> usersPage = new PageImpl<>(List.of(mockUser));
        when(userRepo.findByIsActiveAndEmailContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCase(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(usersPage);

        UserPageResponse responsePage = userService.getAllUsers(0, 20, "", true);

        assertEquals(1, responsePage.getTotalItems());
        verify(userRepo).findByIsActiveAndEmailContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCase(
                any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Cập nhật profile: báo lỗi khi name chỉ có khoảng trắng")
    void updateProfile_Fail_BlankName() {
        when(userRepo.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("   ");

        ApiException ex = assertThrows(ApiException.class, () ->
                userService.updateProfile(mockUser.getEmail(), request)
        );

        assertEquals("INVALID_NAME", ex.getCode());
    }

    @Test
    @DisplayName("Admin không thể tự vô hiệu hóa tài khoản của chính mình")
    void updateUserStatus_Fail_SelfDeactivate() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setIsActive(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                userService.updateUserStatus(1L, request, "user@tmdt.com")
        );

        assertEquals("SELF_DEACTIVATION_FORBIDDEN", ex.getCode());
    }
}
