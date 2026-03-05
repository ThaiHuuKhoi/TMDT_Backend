package com.KhoiCG.TMDT.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken; // Đổi từ 'token' thành 'accessToken'
    private String refreshToken;
}