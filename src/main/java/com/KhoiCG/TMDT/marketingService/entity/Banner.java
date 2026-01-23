package com.KhoiCG.TMDT.marketingService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "banners")
public class Banner {
    @Id
    private String id;

    private String title;       // Tiêu đề (VD: Sale 50%)
    private String description; // Mô tả nhỏ
    private String imageUrl;    // Link ảnh (Cloudinary)
    private String linkUrl;     // Bấm vào thì nhảy đi đâu (VD: /products?category=men)

    private boolean isActive = true; // Ẩn/Hiện
    private int displayOrder;        // Thứ tự hiển thị (1, 2, 3...)
}