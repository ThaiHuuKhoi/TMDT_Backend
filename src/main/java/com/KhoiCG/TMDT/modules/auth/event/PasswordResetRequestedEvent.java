package com.KhoiCG.TMDT.modules.auth.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordResetRequestedEvent {
    private final String email;
    private final String token;
}