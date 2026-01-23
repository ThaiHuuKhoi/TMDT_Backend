package com.KhoiCG.TMDT.paymentService.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private String id; // Product ID (Database ID)
    private String name;
    private int quantity;
    private Double price; // Giá sản phẩm (VD: 20.5)
    private String image;
}
// Lưu ý: Code cũ lấy giá từ Stripe, không tin tưởng giá từ frontend gửi lên