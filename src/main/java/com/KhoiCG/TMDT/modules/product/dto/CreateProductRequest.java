package com.KhoiCG.TMDT.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String shortDescription; // Có thể null

    @NotBlank(message = "Description is required")
    private String description;

    // Frontend gửi số (JSON number), ta hứng bằng Double cho linh hoạt
    @Min(value = 0, message = "Price must be greater than 0")
    private Double price;

    // Frontend gửi Slug, nhưng DB cần Category ID -> Service sẽ xử lý
    @NotBlank(message = "Category slug is required")
    private String categorySlug;

    @NotNull(message = "Sizes are required")
    private List<String> sizes;

    @NotNull(message = "Colors are required")
    private List<String> colors;

    private Map<String, String> images;
}