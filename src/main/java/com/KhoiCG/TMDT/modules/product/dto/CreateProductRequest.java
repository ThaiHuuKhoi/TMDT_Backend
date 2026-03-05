// File: src/main/java/com/KhoiCG/TMDT/modules/product/dto/CreateProductRequest.java
package com.KhoiCG.TMDT.modules.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String shortDescription;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Danh mục không được để trống")
    private String categorySlug;

    private List<String> imageUrls;

    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 biến thể")
    @Valid
    private List<VariantDto> variants;

    @Data
    public static class VariantDto {
        @NotBlank(message = "SKU không được để trống")
        private String sku;

        @NotNull(message = "Giá không được để trống")
        private BigDecimal price;

        @NotNull(message = "Số lượng tồn kho không được để trống")
        private Integer stockQuantity;

        @NotEmpty(message = "Biến thể phải có thuộc tính")
        private Map<String, String> attributes;
    }
}