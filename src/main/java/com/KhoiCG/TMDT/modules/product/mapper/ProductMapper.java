package com.KhoiCG.TMDT.modules.product.mapper;

import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())

                // 1. Map Category
                .category(product.getCategory() != null ? ProductResponse.CategoryDto.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .slug(product.getCategory().getSlug())
                        .image(product.getCategory().getImage())
                        .build() : null)

                // 2. Map Images
                .images(product.getImages() != null ? product.getImages().stream().map(img ->
                        ProductResponse.ImageDto.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .isMain(img.getIsMain())
                                .displayOrder(img.getDisplayOrder())
                                .variantId(img.getVariant() != null ? img.getVariant().getId() : null)
                                .build()
                ).collect(Collectors.toList()) : null)

                // 3. Map Variants & AttributeValues
                .variants(product.getVariants() != null ? product.getVariants().stream().map(var ->
                        ProductResponse.VariantDto.builder()
                                .id(var.getId())
                                .sku(var.getSku())
                                .price(var.getPrice())
                                .stockQuantity(var.getStockQuantity())
                                .version(var.getVersion())
                                .isActive(var.getIsActive())
                                .createdAt(var.getCreatedAt())
                                .updatedAt(var.getUpdatedAt())
                                .attributeValues(var.getAttributeValues() != null ? var.getAttributeValues().stream().map(attrVal ->
                                        ProductResponse.AttributeValueDto.builder()
                                                .id(attrVal.getId())
                                                .value(attrVal.getValue())
                                                .attribute(ProductResponse.AttributeDto.builder()
                                                        .id(attrVal.getAttribute().getId())
                                                        .name(attrVal.getAttribute().getName())
                                                        .build())
                                                .build()
                                ).collect(Collectors.toList()) : null)
                                .build()
                ).collect(Collectors.toList()) : null)
                .build();
    }
}