package com.KhoiCG.TMDT.modules.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private String status;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private CategoryDto category;
    private List<VariantDto> variants;
    private List<ImageDto> images;

    @Data
    @Builder
    public static class CategoryDto {
        private Long id;
        private String name;
        private String slug;
        private String image;
    }

    @Data
    @Builder
    public static class VariantDto {
        private Long id;
        private String sku;
        private BigDecimal price;
        private Integer stockQuantity;
        private Integer version;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<AttributeValueDto> attributeValues;
    }

    @Data
    @Builder
    public static class AttributeValueDto {
        private Long id;
        private String value;
        private AttributeDto attribute;
    }

    @Data
    @Builder
    public static class AttributeDto {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class ImageDto {
        private Long id;
        private String url;
        private Boolean isMain;
        private Integer displayOrder;
        private Long variantId;
    }
}