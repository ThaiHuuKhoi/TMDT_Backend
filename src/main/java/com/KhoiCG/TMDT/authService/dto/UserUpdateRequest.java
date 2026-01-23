package com.KhoiCG.TMDT.authService.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String avatar; // Lưu link ảnh (String)
}