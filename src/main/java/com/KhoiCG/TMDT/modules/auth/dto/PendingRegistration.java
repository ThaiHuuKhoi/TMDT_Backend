package com.KhoiCG.TMDT.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingRegistration {
    private String name;
    private String email;
    private String encodedPassword;
    private String otp;
}
