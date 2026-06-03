package com.KhoiCG.TMDT.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Avatar URL không được vượt quá 500 ký tự")
    @Pattern(
            regexp = "^$|^https?://.+",
            message = "Avatar phải là URL hợp lệ (http:// hoặc https://)"
    )
    private String avatar;
}
