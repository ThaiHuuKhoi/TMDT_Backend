package com.KhoiCG.TMDT.modules.product.dto;

import com.KhoiCG.TMDT.modules.product.entity.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String shortDescription;

    private String description;

    private String categorySlug;

    private ProductStatus status;

    private List<String> imageUrls;
}
