package com.KhoiCG.TMDT.modules.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FacebookPostRequest {
    @NotBlank(message = "Nội dung bài đăng không được để trống")
    @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự")
    private String message;

    @Size(max = 500, message = "Link tối đa 500 ký tự")
    private String linkUrl;

    @Size(max = 500, message = "Ảnh tối đa 500 ký tự")
    private String imageUrl;
}
