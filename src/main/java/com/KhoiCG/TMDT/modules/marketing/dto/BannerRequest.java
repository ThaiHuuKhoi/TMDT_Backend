package com.KhoiCG.TMDT.modules.marketing.dto;

import com.KhoiCG.TMDT.modules.marketing.entity.TargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BannerRequest {
    @NotBlank(message = "title là bắt buộc")
    @Size(max = 200, message = "title tối đa 200 ký tự")
    private String title;

    @Size(max = 2000, message = "description tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "imageUrl là bắt buộc")
    @Size(max = 500, message = "imageUrl tối đa 500 ký tự")
    private String imageUrl;

    @NotNull(message = "targetType là bắt buộc")
    private TargetType targetType;
    private Long targetId;

    @Size(max = 500, message = "linkUrl tối đa 500 ký tự")
    private String linkUrl;

    @Min(value = 0, message = "displayOrder phải lớn hơn hoặc bằng 0")
    private Integer displayOrder;
    private Boolean isActive;
}