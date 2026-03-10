package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.CreateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.*;
import com.KhoiCG.TMDT.modules.product.enums.ProductSortType;
import com.KhoiCG.TMDT.modules.product.mapper.ProductMapper;
import com.KhoiCG.TMDT.modules.product.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    @Transactional
    public Product createProduct(CreateProductRequest req) {
        Category category = categoryRepository.findBySlug(req.getCategorySlug())
                .orElseThrow(() -> new RuntimeException("Category không tồn tại: " + req.getCategorySlug()));

        for (CreateProductRequest.VariantDto v : req.getVariants()) {
            if (variantRepository.existsBySku(v.getSku())) {
                throw new RuntimeException("SKU đã tồn tại trong hệ thống: " + v.getSku());
            }
        }

        String productSlug = generateUniqueSlug(req.getName());

        Product product = Product.builder()
                .name(req.getName())
                .slug(productSlug)
                .description(req.getDescription())
                .shortDescription(req.getShortDescription())
                .category(category)
                .status(ProductStatus.ACTIVE)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(req.getImageUrls().get(i))
                        .isMain(i == 0)
                        .displayOrder(i)
                        .build();
                product.getImages().add(image);
            }
        }

        for (CreateProductRequest.VariantDto variantDto : req.getVariants()) {
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(variantDto.getSku())
                    .price(variantDto.getPrice())
                    .stockQuantity(variantDto.getStockQuantity())
                    .attributeValues(new ArrayList<>())
                    .build();

            for (Map.Entry<String, String> entry : variantDto.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                String attrValueStr = entry.getValue();

                Attribute attribute = attributeRepository.findByNameIgnoreCase(attrName)
                        .orElseGet(() -> attributeRepository.save(Attribute.builder().name(attrName).build()));

                AttributeValue attributeValue = attributeValueRepository
                        .findByAttributeIdAndValueIgnoreCase(attribute.getId(), attrValueStr)
                        .orElseGet(() -> attributeValueRepository.save(
                                AttributeValue.builder().attribute(attribute).value(attrValueStr).build()
                        ));

                variant.getAttributeValues().add(attributeValue);
            }
            product.getVariants().add(variant);
        }

        return productRepository.save(product);
    }

    public List<ProductResponse> getProducts(String categorySlug, String search, String sortStr, int limit) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(categorySlug)) {
                predicates.add(cb.equal(root.get("category").get("slug"), categorySlug));
            }
            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = ProductSortType.getSortStrategy(sortStr);
        Pageable pageable = limit > 0 ? PageRequest.of(0, limit, sort) : PageRequest.of(0, 100, sort);

        List<Product> products = productRepository.findAll(spec, pageable).getContent();
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "product_details", key = "#id")
    public void deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            log.info("Đã xóa cache của sản phẩm ID: {}", id);
        } else {
            throw new RuntimeException("Product not found");
        }
    }

    @Cacheable(value = "product_details", key = "#id")
    public ProductResponse getProduct(Long id) {
        log.info("🚀 [CACHE MISS] - Phải query Database để lấy sản phẩm ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return productMapper.toProductResponse(product);
    }

    @Cacheable(value = "product_details", key = "'related_' + #currentProductId")
    public List<ProductResponse> getRelatedProducts(Long currentProductId) {
        log.info("🚀 [CACHE MISS] - Phải query Database để lấy sản phẩm liên quan cho ID: {}", currentProductId);
        ProductResponse currentProduct = getProduct(currentProductId);
        Long categoryId = currentProduct.getCategory().getId();

        List<Product> related = productRepository.findByCategoryIdAndIdNot(
                categoryId, currentProductId, PageRequest.of(0, 4)
        );
        return related.stream().map(productMapper::toProductResponse).toList();
    }

    private String generateUniqueSlug(String name) {
        String slug = toSlug(name);
        if (productRepository.existsBySlug(slug)) {
            slug += "-" + System.currentTimeMillis();
        }
        return slug;
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("[\\s]").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}