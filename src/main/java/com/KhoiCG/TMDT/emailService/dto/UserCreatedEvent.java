package com.KhoiCG.TMDT.emailService.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserCreatedEvent {
    private String email;
    private String username;
    // Các trường khác nếu có
}