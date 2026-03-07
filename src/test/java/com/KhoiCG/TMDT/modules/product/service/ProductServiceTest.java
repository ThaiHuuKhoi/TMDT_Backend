package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.CreateProductRequest;
import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.*;
import com.KhoiCG.TMDT.modules.product.mapper.ProductMapper;
import com.KhoiCG.TMDT.modules.product.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private AttributeRepository attributeRepository;
    @Mock private AttributeValueRepository attributeValueRepository;

    @InjectMocks
    private ProductService productService;

    private Category mockCategory;
    private Product mockProduct;
    private ProductResponse mockProductResponse;

    @BeforeEach
    void setUp() {
        mockCategory = new Category(1L, "Điện thoại", "dien-thoai", "icon.png");

        mockProduct = Product.builder()
                .id(100L)
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .category(mockCategory)
                .status(ProductStatus.ACTIVE)
                .build();

        mockProductResponse = ProductResponse.builder()
                .id(100L)
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .build();
    }

    // ==========================================
    // 1. TEST TẠO SẢN PHẨM MỚI
    // ==========================================

    @Test
    @DisplayName("Tạo Sản phẩm: Thành công, map đúng các thuộc tính, biến thể và hình ảnh")
    void createProduct_Success() {
        // Arrange - Chuẩn bị Request
        CreateProductRequest request = new CreateProductRequest();
        request.setName("iPhone 15 Pro Max");
        request.setCategorySlug("dien-thoai");
        request.setDescription("Siêu phẩm Apple");
        request.setImageUrls(List.of("img1.png", "img2.png"));

        CreateProductRequest.VariantDto variantDto = new CreateProductRequest.VariantDto();
        variantDto.setSku("IP15-256-TITAN");
        variantDto.setPrice(new BigDecimal("30000000"));
        variantDto.setStockQuantity(50);
        variantDto.setAttributes(Map.of("Màu sắc", "Titan Tự Nhiên", "Dung lượng", "256GB"));
        request.setVariants(List.of(variantDto));

        // Mock các Repositories
        when(categoryRepository.findBySlug("dien-thoai")).thenReturn(Optional.of(mockCategory));
        when(variantRepository.existsBySku("IP15-256-TITAN")).thenReturn(false);
        when(productRepository.existsBySlug(anyString())).thenReturn(false);

        // Mock Attribute & AttributeValue (Giả sử chưa có trong DB, phải tạo mới)
        when(attributeRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(attributeRepository.save(any(Attribute.class))).thenAnswer(i -> {
            Attribute attr = i.getArgument(0);
            attr.setId(1L);
            return attr;
        });

        when(attributeValueRepository.findByAttributeIdAndValueIgnoreCase(anyLong(), anyString())).thenReturn(Optional.empty());
        when(attributeValueRepository.save(any(AttributeValue.class))).thenAnswer(i -> i.getArgument(0));

        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Product result = productService.createProduct(request);

        // Assert
        assertNotNull(result);
        assertEquals("iPhone 15 Pro Max", result.getName());
        assertEquals("iphone-15-pro-max", result.getSlug()); // Kiểm tra hàm gen Slug

        // Kiểm tra hình ảnh được thêm đúng không (img1 là main)
        assertEquals(2, result.getImages().size());
        assertTrue(result.getImages().get(0).getIsMain());
        assertFalse(result.getImages().get(1).getIsMain());

        // Kiểm tra biến thể và thuộc tính
        assertEquals(1, result.getVariants().size());
        assertEquals("IP15-256-TITAN", result.getVariants().get(0).getSku());
        assertEquals(2, result.getVariants().get(0).getAttributeValues().size()); // 2 thuộc tính: Màu sắc & Dung lượng
    }

    @Test
    @DisplayName("Tạo Sản phẩm: Ném lỗi nếu Category Slug không tồn tại")
    void createProduct_Fail_CategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest();
        request.setCategorySlug("fake-category");

        when(categoryRepository.findBySlug("fake-category")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> productService.createProduct(request));
        assertEquals("Category không tồn tại: fake-category", ex.getMessage());
    }

    @Test
    @DisplayName("Tạo Sản phẩm: Ném lỗi nếu SKU của biến thể đã tồn tại")
    void createProduct_Fail_SkuExists() {
        CreateProductRequest request = new CreateProductRequest();
        request.setCategorySlug("dien-thoai");
        CreateProductRequest.VariantDto variant = new CreateProductRequest.VariantDto();
        variant.setSku("DUPLICATE-SKU");
        request.setVariants(List.of(variant));

        when(categoryRepository.findBySlug("dien-thoai")).thenReturn(Optional.of(mockCategory));
        when(variantRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        Exception ex = assertThrows(RuntimeException.class, () -> productService.createProduct(request));
        assertEquals("SKU đã tồn tại trong hệ thống: DUPLICATE-SKU", ex.getMessage());
    }

    // ==========================================
    // 2. TEST TÌM KIẾM & LẤY DANH SÁCH (SPECIFICATION)
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách Sản phẩm: Hỗ trợ tìm kiếm, phân trang và sắp xếp")
    void getProducts_Success() {
        // Arrange
        Page<Product> mockPage = new PageImpl<>(List.of(mockProduct));
        // Ép kiểu ép buộc Mockito hiểu là chúng ta đang mock hàm findAll nhận Specification và Pageable
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        // Act
        List<ProductResponse> results = productService.getProducts("dien-thoai", "iphone", "desc", 10);

        // Assert
        assertEquals(1, results.size());
        assertEquals("iPhone 15 Pro Max", results.get(0).getName());
        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==========================================
    // 3. TEST CHI TIẾT & SẢN PHẨM LIÊN QUAN
    // ==========================================

    @Test
    @DisplayName("Lấy chi tiết Sản phẩm: Thành công và map qua DTO")
    void getProduct_Success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);

        ProductResponse result = productService.getProduct(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    @DisplayName("Lấy Sản phẩm liên quan: Lấy cùng Category nhưng loại trừ ID hiện tại")
    void getRelatedProducts_Success() {
        // Arrange
        Product relatedProduct = Product.builder().id(101L).build();
        ProductResponse relatedResponse = ProductResponse.builder().id(101L).build();

        // 1. Phải mock hàm getProduct (được gọi ngầm bên trong getRelatedProducts)
        when(productRepository.findById(100L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.toProductResponse(mockProduct)).thenReturn(mockProductResponse);
        // Lưu ý: Do mockProductResponse ở SetUp chưa có CategoryDto, ta cần add tay vào để tránh NullPointer
        mockProductResponse.setCategory(ProductResponse.CategoryDto.builder().id(1L).build());

        // 2. Mock hàm tìm kiếm sản phẩm liên quan
        when(productRepository.findByCategoryIdAndIdNot(eq(1L), eq(100L), any(Pageable.class)))
                .thenReturn(List.of(relatedProduct));
        when(productMapper.toProductResponse(relatedProduct)).thenReturn(relatedResponse);

        // Act
        List<ProductResponse> results = productService.getRelatedProducts(100L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getId());
    }

    // ==========================================
    // 4. TEST XÓA SẢN PHẨM
    // ==========================================

    @Test
    @DisplayName("Xóa Sản phẩm: Xóa thành công và dọn Cache")
    void deleteProduct_Success() {
        when(productRepository.existsById(100L)).thenReturn(true);

        productService.deleteProduct(100L);

        verify(productRepository, times(1)).deleteById(100L);
    }
}