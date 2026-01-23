package com.KhoiCG.TMDT.productService.service;

import com.KhoiCG.TMDT.productService.dto.ProductCreateRequest;
import com.KhoiCG.TMDT.productService.dto.StripeProductDto;
import com.KhoiCG.TMDT.productService.entity.Category;
import com.KhoiCG.TMDT.productService.entity.Product;
import com.KhoiCG.TMDT.productService.repository.CategoryRepository;
import com.KhoiCG.TMDT.productService.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    // --- Create Product ---
    public Product createProduct(ProductCreateRequest req) {
        // 1. Validate Colors vs Images (Logic giống Node.js)
        if (req.getColors() == null || req.getColors().isEmpty()) {
            throw new IllegalArgumentException("Colors array is required!");
        }
        if (req.getImages() == null) {
            throw new IllegalArgumentException("Images object is required!");
        }

        List<String> missingColors = req.getColors().stream()
                .filter(color -> !req.getImages().containsKey(color))
                .toList();

        if (!missingColors.isEmpty()) {
            throw new IllegalArgumentException("Missing images for colors: " + missingColors);
        }

        // 2. Mapping Entity
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = new Product();
        product.setName(req.getName());
        product.setPrice(req.getPrice());
        product.setDescription(req.getDescription());
        product.setSlug(req.getSlug());
        product.setColors(req.getColors());
        product.setImages(req.getImages());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // 3. Send Kafka Event
        StripeProductDto stripeDto = new StripeProductDto(
                savedProduct.getId().toString(),
                savedProduct.getName(),
                savedProduct.getPrice()
        );
//        kafkaTemplate.send("product.created", stripeDto);

        return savedProduct;
    }

    // --- Get Products with Filters ---
    public List<Product> getProducts(String categorySlug, String search, String sortStr, int limit) {

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
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Default
        if ("asc".equals(sortStr)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("desc".equals(sortStr)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("oldest".equals(sortStr)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        // 3. Xử lý Limit (Phân trang)
        Pageable pageable = limit > 0 ? PageRequest.of(0, limit, sort) : PageRequest.of(0, 100, sort);

        return productRepository.findAll(spec, pageable).getContent();
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
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }
}