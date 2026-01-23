package com.KhoiCG.TMDT.authService.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;    // Dùng email làm username đăng nhập
    private String password;
}