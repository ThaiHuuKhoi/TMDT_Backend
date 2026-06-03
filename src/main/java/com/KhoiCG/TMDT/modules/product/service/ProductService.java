package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.coupon.service.CouponService;
import com.KhoiCG.TMDT.modules.product.dto.CreateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.ProductPageResponse;
import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.dto.UpdateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.UpdateVariantRequest;
import com.KhoiCG.TMDT.modules.product.entity.*;
import com.KhoiCG.TMDT.modules.product.enums.ProductSortType;
import com.KhoiCG.TMDT.modules.product.mapper.ProductMapper;
import com.KhoiCG.TMDT.modules.product.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern NON_WORD = Pattern.compile("[^\\w-]");

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final CouponService couponService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        Category category = findCategoryBySlug(req.getCategorySlug());

        List<String> requestedSkus = req.getVariants().stream()
                .map(CreateProductRequest.VariantDto::getSku)
                .toList();
        List<String> duplicateSkus = variantRepository.findExistingSkus(requestedSkus);
        if (!duplicateSkus.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU_ALREADY_EXISTS",
                    "SKU đã tồn tại: " + duplicateSkus);
        }

        Product product = Product.builder()
                .name(req.getName())
                .slug(generateUniqueSlug(req.getName()))
                .description(req.getDescription())
                .shortDescription(req.getShortDescription())
                .category(category)
                .status(ProductStatus.ACTIVE)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        buildImages(product, req.getImageUrls());

        for (CreateProductRequest.VariantDto variantDto : req.getVariants()) {
            product.getVariants().add(buildVariant(product, variantDto));
        }

        return attachVariantPromotions(productMapper.toProductResponse(productRepository.save(product)));
    }

    public ProductPageResponse getProducts(
            String categorySlug, String search, String sortStr, int page, int size,
            BigDecimal minPrice, BigDecimal maxPrice, Double minRating, ProductStatus status) {

        ProductSortType sortType = ProductSortType.fromCode(sortStr);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(categorySlug)) {
                predicates.add(cb.equal(root.get("category").get("slug"), categorySlug));
            }
            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }

            // Subquery lấy MIN(variant.price) — dùng cho cả filter và sort theo giá
            Subquery<BigDecimal> minPriceSub = null;
            if (minPrice != null || maxPrice != null || sortType.isPriceBased()) {
                minPriceSub = query.subquery(BigDecimal.class);
                Root<ProductVariant> varRoot = minPriceSub.from(ProductVariant.class);
                minPriceSub.select(cb.min(varRoot.get("price")))
                        .where(cb.equal(varRoot.get("product"), root), cb.isTrue(varRoot.get("isActive")));
                if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(minPriceSub, minPrice));
                if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(minPriceSub, maxPrice));
            }

            // Sort theo giá qua subquery — chỉ áp dụng cho data query, không phải count query
            if (sortType.isPriceBased() && !Long.class.equals(query.getResultType())) {
                query.orderBy(sortType == ProductSortType.PRICE_ASC
                        ? cb.asc(minPriceSub)
                        : cb.desc(minPriceSub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        // Price sort được handle trong Specification — truyền Sort.unsorted() để tránh conflict
        Sort sort = sortType.isPriceBased() ? Sort.unsorted() : sortType.getSort();
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponse> items = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .map(this::attachVariantPromotions)
                .toList();

        return ProductPageResponse.builder()
                .items(items)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalItems(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_details", key = "#id"),
            @CacheEvict(value = "related_products", key = "'related_' + #id")
    })
    public ProductResponse updateProduct(Long id, UpdateProductRequest req) {
        Product product = findProductById(id);

        product.setName(req.getName());
        product.setShortDescription(req.getShortDescription());
        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            product.setDescription(req.getDescription());
        }
        if (req.getCategorySlug() != null && !req.getCategorySlug().isBlank()) {
            product.setCategory(findCategoryBySlug(req.getCategorySlug()));
        }
        if (req.getStatus() != null) {
            product.setStatus(req.getStatus());
        }
        if (req.getImageUrls() != null) {
            product.getImages().clear();
            buildImages(product, req.getImageUrls());
        }

        return attachVariantPromotions(productMapper.toProductResponse(productRepository.save(product)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product_details", key = "#id"),
            @CacheEvict(value = "related_products", key = "'related_' + #id")
    })
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        productRepository.delete(product);
    }

    @Cacheable(value = "product_details", key = "#id")
    public ProductResponse getProduct(Long id) {
        log.info("[CACHE MISS] product id={}", id);
        return attachVariantPromotions(productMapper.toProductResponse(findProductById(id)));
    }

    @Cacheable(value = "related_products", key = "'related_' + #currentProductId")
    public List<ProductResponse> getRelatedProducts(Long currentProductId) {
        log.info("[CACHE MISS] related products for id={}", currentProductId);
        Product current = findProductById(currentProductId);
        return productRepository
                .findByCategoryIdAndIdNot(current.getCategory().getId(), currentProductId, PageRequest.of(0, 4))
                .stream()
                .map(productMapper::toProductResponse)
                .map(this::attachVariantPromotions)
                .toList();
    }

    @Transactional
    public ProductResponse.VariantDto updateVariant(Long variantId, UpdateVariantRequest req) {
        ProductVariant variant = findVariantById(variantId);
        variant.setPrice(req.getPrice());
        variant.setOriginalPrice(req.getOriginalPrice());
        variant.setStockQuantity(req.getStockQuantity());
        if (req.getIsActive() != null) {
            variant.setIsActive(req.getIsActive());
        }
        return productMapper.toVariantDto(variantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = findVariantById(variantId);
        if (variant.getProduct().getVariants().size() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LAST_VARIANT", "Sản phẩm phải có ít nhất 1 biến thể.");
        }
        variantRepository.delete(variant);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm."));
    }

    private ProductVariant findVariantById(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND", "Không tìm thấy biến thể."));
    }

    private Category findCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND",
                        "Category không tồn tại: " + slug));
    }

    private void buildImages(Product product, List<String> urls) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .url(urls.get(i))
                    .isMain(i == 0)
                    .displayOrder(i)
                    .build());
        }
    }

    private ProductVariant buildVariant(Product product, CreateProductRequest.VariantDto dto) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(dto.getSku())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .stockQuantity(dto.getStockQuantity())
                .attributeValues(new ArrayList<>())
                .build();

        for (Map.Entry<String, String> entry : dto.getAttributes().entrySet()) {
            Attribute attribute = attributeRepository.findByNameIgnoreCase(entry.getKey())
                    .orElseGet(() -> attributeRepository.save(
                            Attribute.builder().name(entry.getKey()).build()));

            AttributeValue attrValue = attributeValueRepository
                    .findByAttributeIdAndValueIgnoreCase(attribute.getId(), entry.getValue())
                    .orElseGet(() -> attributeValueRepository.save(
                            AttributeValue.builder().attribute(attribute).value(entry.getValue()).build()));

            variant.getAttributeValues().add(attrValue);
        }
        return variant;
    }

    private ProductResponse attachVariantPromotions(ProductResponse response) {
        if (response.getVariants() == null) return response;
        for (ProductResponse.VariantDto v : response.getVariants()) {
            if (v.getPrice() == null || Boolean.FALSE.equals(v.getIsActive())) continue;
            couponService.findBestOfferForOrderAmount(v.getPrice().longValue()).ifPresent(offer ->
                    v.setBestPromo(ProductResponse.BestPromoDto.builder()
                            .code(offer.getCode())
                            .discountAmount(offer.getDiscountAmount())
                            .finalPrice(offer.getFinalPrice())
                            .build()));
        }
        return response;
    }

    private String generateUniqueSlug(String name) {
        String slug = toSlug(name);
        return productRepository.existsBySlug(slug) ? slug + "-" + System.currentTimeMillis() : slug;
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String dashed = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(dashed, Normalizer.Form.NFD);
        return NON_WORD.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
    }
}
