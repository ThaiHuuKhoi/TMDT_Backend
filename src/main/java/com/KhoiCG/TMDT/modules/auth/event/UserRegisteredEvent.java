package com.KhoiCG.TMDT.modules.auth.event;

import com.KhoiCG.TMDT.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegisteredEvent {
    private final User user;
}