package com.KhoiCG.TMDT.modules.product.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;

    private String userId;      // Ai đánh giá?
    private String userName;    // Tên người đánh giá (lưu luôn để đỡ query lại User)
    private String userAvatar;  // Avatar (nếu có)

    private String productId;   // Đánh giá sản phẩm nào?

    private int rating;         // 1 đến 5
    private String comment;     // Nội dung: "Hàng đẹp quá shop ơi!"

    @CreatedDate
    private LocalDateTime createdAt;
}