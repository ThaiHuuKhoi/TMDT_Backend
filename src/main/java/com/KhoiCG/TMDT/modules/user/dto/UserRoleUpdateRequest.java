package com.KhoiCG.TMDT.modules.user.dto;

import com.KhoiCG.TMDT.modules.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {

    @NotNull(message = "Role không được để trống")
    private Role role;
}
