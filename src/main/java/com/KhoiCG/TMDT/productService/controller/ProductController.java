package com.KhoiCG.TMDT.productService.controller;

import com.KhoiCG.TMDT.productService.dto.ProductCreateRequest;
import com.KhoiCG.TMDT.productService.entity.Product;
import com.KhoiCG.TMDT.productService.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") int limit
    ) {
        return ResponseEntity.ok(productService.getProducts(category, search, sort, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    // @PreAuthorize("hasAuthority('ADMIN')") // Mở comment nếu đã cấu hình Security
    public ResponseEntity<Product> createProduct(@RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // Update method tương tự...
}