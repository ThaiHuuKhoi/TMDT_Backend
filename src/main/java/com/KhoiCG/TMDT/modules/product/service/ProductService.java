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
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional // Bắt buộc phải có để rollback nếu 1 bước bị lỗi
    public Product createProduct(CreateProductRequest req) {

        // 1. Tìm Category
        Category category = categoryRepository.findBySlug(req.getCategorySlug())
                .orElseThrow(() -> new RuntimeException("Category không tồn tại: " + req.getCategorySlug()));

        // 2. Xử lý trùng lặp SKU trước khi tạo
        for (CreateProductRequest.VariantDto v : req.getVariants()) {
            if (variantRepository.existsBySku(v.getSku())) {
                throw new RuntimeException("SKU đã tồn tại trong hệ thống: " + v.getSku());
            }
        }

        // 3. Khởi tạo đối tượng Product chính
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

        // 4. Xử lý Hình ảnh chung (Product Images)
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

        // 5. Xử lý các Biến thể (Variants) & Thuộc tính (Attributes)
        for (CreateProductRequest.VariantDto variantDto : req.getVariants()) {

            // Khởi tạo Biến thể
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(variantDto.getSku())
                    .price(variantDto.getPrice())
                    .stockQuantity(variantDto.getStockQuantity())
                    .attributeValues(new ArrayList<>())
                    .build();

            // Duyệt qua map Thuộc tính (VD: "Color" -> "Red", "Size" -> "XL")
            for (Map.Entry<String, String> entry : variantDto.getAttributes().entrySet()) {
                String attrName = entry.getKey();
                String attrValueStr = entry.getValue();

                // 5.1 Lấy hoặc Tạo mới Attribute (VD: "Color")
                Attribute attribute = attributeRepository.findByNameIgnoreCase(attrName)
                        .orElseGet(() -> attributeRepository.save(Attribute.builder().name(attrName).build()));

                // 5.2 Lấy hoặc Tạo mới AttributeValue (VD: "Red" thuộc "Color")
                AttributeValue attributeValue = attributeValueRepository
                        .findByAttributeIdAndValueIgnoreCase(attribute.getId(), attrValueStr)
                        .orElseGet(() -> attributeValueRepository.save(
                                AttributeValue.builder().attribute(attribute).value(attrValueStr).build()
                        ));

                // Gắn giá trị thuộc tính vào biến thể này
                variant.getAttributeValues().add(attributeValue);
            }

            product.getVariants().add(variant);
        }

        // 6. Lưu toàn bộ (Nhờ CascadeType.ALL, Product, Images và Variants sẽ tự động được lưu cùng nhau)
        Product savedProduct = productRepository.save(product);

        //   kafkaTemplate.send("product.created", stripeDto);

        return savedProduct;
    }

    // --- Get Products with Filters ---
    public List<ProductResponse> getProducts(String categorySlug, String search, String sortStr, int limit) {

        // 1. Xây dựng Specification (Dynamic Query WHERE clause)
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by Category Slug
            if (StringUtils.hasText(categorySlug)) {
                // Join bảng Category để check slug
                predicates.add(cb.equal(root.get("category").get("slug"), categorySlug));
            }

            // Filter by Search Name (Insensitive)
            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. Xử lý Sort
        Sort sort = ProductSortType.getSortStrategy(sortStr);

        // 3. Xử lý Limit (Phân trang)
        Pageable pageable = limit > 0 ? PageRequest.of(0, limit, sort) : PageRequest.of(0, 100, sort);

        List<Product> products = productRepository.findAll(spec, pageable).getContent();
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    // --- Delete Product ---
    public void deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            // Kafka send ID as String or Long depending on Consumer expectation
            // Node code gửi: { value: Number(id) } -> Java gửi Long
//            kafkaTemplate.send("product.deleted", id.toString());
        } else {
            throw new RuntimeException("Product not found");
        }
    }

    // --- Update, Get One... (Tương tự) ---
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return productMapper.toProductResponse(product);
    }

    public List<ProductResponse> getRelatedProducts(Long currentProductId) {
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