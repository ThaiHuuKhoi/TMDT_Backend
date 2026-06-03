package com.KhoiCG.TMDT.modules.product.mapper;

import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.Category;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.ProductImage;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .category(mapCategory(product.getCategory()))
                .images(mapImages(product.getImages()))
                .variants(mapVariants(product.getVariants()))
                .build();
    }

    public ProductResponse.VariantDto toVariantDto(ProductVariant var) {
        if (var == null) return null;
        return ProductResponse.VariantDto.builder()
                .id(var.getId())
                .sku(var.getSku())
                .price(var.getPrice())
                .originalPrice(var.getOriginalPrice())
                .stockQuantity(var.getStockQuantity())
                .version(var.getVersion())
                .isActive(var.getIsActive())
                .createdAt(var.getCreatedAt())
                .updatedAt(var.getUpdatedAt())
                .attributeValues(mapAttributeValues(var))
                .build();
    }

    private ProductResponse.CategoryDto mapCategory(Category category) {
        if (category == null) return null;
        return ProductResponse.CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .image(category.getImage())
                .build();
    }

    private List<ProductResponse.ImageDto> mapImages(List<com.KhoiCG.TMDT.modules.product.entity.ProductImage> images) {
        if (images == null) return null;
        return images.stream().map(this::mapImage).toList();
    }

    private ProductResponse.ImageDto mapImage(ProductImage img) {
        return ProductResponse.ImageDto.builder()
                .id(img.getId())
                .url(img.getUrl())
                .isMain(img.getIsMain())
                .displayOrder(img.getDisplayOrder())
                .variantId(img.getVariant() != null ? img.getVariant().getId() : null)
                .build();
    }

    private List<ProductResponse.VariantDto> mapVariants(List<ProductVariant> variants) {
        if (variants == null) return null;
        return variants.stream().map(this::toVariantDto).toList();
    }

    private List<ProductResponse.AttributeValueDto> mapAttributeValues(ProductVariant var) {
        if (var.getAttributeValues() == null) return null;
        return var.getAttributeValues().stream().map(attrVal ->
                ProductResponse.AttributeValueDto.builder()
                        .id(attrVal.getId())
                        .value(attrVal.getValue())
                        .attribute(ProductResponse.AttributeDto.builder()
                                .id(attrVal.getAttribute().getId())
                                .name(attrVal.getAttribute().getName())
                                .build())
                        .build()
        ).toList();
    }
}
