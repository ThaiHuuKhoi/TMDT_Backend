package com.KhoiCG.TMDT.orderService.dto;

import com.KhoiCG.TMDT.paymentService.dto.CartItemDto; // Hoặc CheckoutItemDto tùy bạn đặt tên
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    private List<CartItemDto> items; // Danh sách sản phẩm
    private Object shippingForm;     // Thông tin ship (nếu cần)
    private String couponCode;       // 👇 Mã giảm giá từ Frontend
}