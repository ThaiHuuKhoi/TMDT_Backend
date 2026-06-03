package com.KhoiCG.TMDT.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpsertRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    private String slug;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    private Boolean isActive;

    @Size(max = 255, message = "Ảnh không được vượt quá 255 ký tự")
    private String image;
}
