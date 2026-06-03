package com.KhoiCG.TMDT.modules.user.controller;

import com.KhoiCG.TMDT.modules.user.dto.UserPageResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserRoleUpdateRequest;
import com.KhoiCG.TMDT.modules.user.dto.UserStatusUpdateRequest;
import com.KhoiCG.TMDT.modules.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import com.KhoiCG.TMDT.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.getCurrentUser(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody @Valid UserUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.updateProfile(email, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserPageResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, q, isActive));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserByIdForAdmin(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestBody @Valid UserStatusUpdateRequest request) {
        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.updateUserStatus(id, request, actorEmail));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody @Valid UserRoleUpdateRequest request) {
        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.updateUserRole(id, request, actorEmail));
    }
}