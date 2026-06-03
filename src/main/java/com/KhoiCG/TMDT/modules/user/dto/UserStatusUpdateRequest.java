package com.KhoiCG.TMDT.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull(message = "isActive là bắt buộc")
    private Boolean isActive;
}
