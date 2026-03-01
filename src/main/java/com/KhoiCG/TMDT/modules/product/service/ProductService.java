package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.CreateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.StripeProductDto;
import com.KhoiCG.TMDT.modules.product.entity.Category;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.repository.CategoryRepository;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Product createProduct(CreateProductRequest req) {

        // 1. Validate Colors vs Images
        if (req.getColors() == null || req.getColors().isEmpty()) {
            throw new IllegalArgumentException("Colors array is required!");
        }
        // ... giữ logic validate ảnh ...

        // 2. Xử lý Category: Frontend gửi Slug -> Ta tìm Category Entity
        Category category = categoryRepository.findBySlug(req.getCategorySlug())
                .orElseThrow(() -> new RuntimeException("Category not found with slug: " + req.getCategorySlug()));

        // 3. Tạo Slug cho Product
        String productSlug = toSlug(req.getName());
        if (productRepository.existsBySlug(productSlug)) {
            productSlug += "-" + System.currentTimeMillis();
        }

        // 4. MAP DTO -> ENTITY (Giữ nguyên cấu trúc cũ của bạn)
        Product product = new Product();
        product.setName(req.getName());
        product.setSlug(productSlug);

        // ⚠️ Chuyển đổi an toàn: Double (DTO) -> Long (Entity)
        // Nếu req.getPrice() là 19.99 -> lưu thành 19 (hoặc logic nhân 100 tùy bạn)
        // Ở đây mình ép kiểu trực tiếp về Long theo code cũ của bạn
        product.setPrice(req.getPrice().longValue());

        product.setShortDescription(req.getShortDescription());
        product.setDescription(req.getDescription());
        product.setCategory(category); // Set object Category đã tìm được
        product.setColors(req.getColors());
        product.setSizes(req.getSizes());
        product.setImages(req.getImages());
        product.setIsPopular(false);

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

    public List<Product> getRelatedProducts(String currentProductId) {
        // 1. Lấy sản phẩm hiện tại để biết nó thuộc Category nào
        Product currentProduct = getProduct(Long.valueOf(currentProductId));
        Long categoryId = Long.valueOf(currentProduct.getCategory().getId());

        // 2. Lấy 4 sản phẩm cùng loại (trừ chính nó)
        // PageRequest.of(0, 4) -> Lấy trang đầu, 4 phần tử
        List<Product> related = productRepository.findByCategory_IdAndIdNot(
                String.valueOf(categoryId),
                currentProductId,
                PageRequest.of(0, 4)
        );

        // Mẹo nhỏ: Nếu muốn ngẫu nhiên (Random), bạn có thể lấy 20 cái rồi dùng Java Collections.shuffle()
        // Nhưng để hiệu năng cao thì lấy 4 cái mới nhất là ổn.

        return related;
    }
    private String toSlug(String input) {
        if (input == null) return "";
        // Thay thế khoảng trắng bằng dấu gạch ngang
        String nowhitespace = Pattern.compile("[\\s]").matcher(input).replaceAll("-");
        // Chuẩn hóa chuỗi (loại bỏ dấu tiếng Việt)
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        // Loại bỏ các ký tự đặc biệt không phải chữ cái, số hoặc gạch ngang
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        // Chuyển về chữ thường
        return slug.toLowerCase(Locale.ENGLISH);
    }
}