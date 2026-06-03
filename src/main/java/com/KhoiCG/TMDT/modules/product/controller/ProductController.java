package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.product.dto.CreateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.ProductPageResponse;
import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.dto.UpdateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.UpdateVariantRequest;
import com.KhoiCG.TMDT.modules.product.entity.ProductStatus;
import com.KhoiCG.TMDT.modules.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductPageResponse> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) ProductStatus status
    ) {
        return ResponseEntity.ok(productService.getProducts(category, search, sort, page, size, minPrice, maxPrice, minRating, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductResponse>> getRelatedProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getRelatedProducts(id));
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse.VariantDto> updateVariant(
            @PathVariable Long variantId,
            @RequestBody @Valid UpdateVariantRequest request) {
        return ResponseEntity.ok(productService.updateVariant(variantId, request));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteVariant(@PathVariable Long variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.ok().build();
    }

}