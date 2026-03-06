// File: src/main/java/com/KhoiCG/TMDT/modules/order/mapper/OrderMapper.java
package com.KhoiCG.TMDT.modules.order.mapper;

import com.KhoiCG.TMDT.modules.order.dto.OrderItemResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.dto.UserOrderDTO;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    // Format chuẩn quốc tế ISO 8601 để Frontend (new Date()) dễ đọc nhất
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) return null;

        // Bọc thông tin User vào DTO an toàn
        UserOrderDTO userDto = UserOrderDTO.builder()
                .id(order.getUser().getId())
                .name(order.getUser().getName())
                .email(order.getUser().getEmail())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .user(userDto)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO_FORMATTER) : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .totalAmount(order.getTotalAmount()) // Trả về số thực
                .totalAmountFormatted(String.format("%,.0f VNĐ", order.getTotalAmount()))
                .shippingAddress(order.getShippingAddress())
                .stripeSessionId(order.getStripeSessionId())
                .items(order.getItems() != null ?
                        order.getItems().stream().map(this::toOrderItemResponse).collect(Collectors.toList())
                        : null)
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) return null;

        // 🔥 Logic moi móc ảnh từ Variant -> Product -> Image list
        String imageUrl = "/product-placeholder.png"; // Ảnh mặc định
        if (item.getVariant() != null &&
                item.getVariant().getProduct() != null &&
                item.getVariant().getProduct().getImages() != null &&
                !item.getVariant().getProduct().getImages().isEmpty()) {

            // Ưu tiên lấy ảnh đầu tiên (Hoặc bạn có thể viết luồng lặp tìm ảnh isMain)
            imageUrl = item.getVariant().getProduct().getImages().get(0).getUrl();
        }

        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .variantInfo(item.getVariant() != null ? "SKU: " + item.getVariant().getSku() : "")
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase() != null ? item.getPriceAtPurchase().longValue() : 0L)
                .priceFormatted(String.format("%,.0f VNĐ", item.getPriceAtPurchase()))
                .productImage(imageUrl) //
                .build();
    }
}