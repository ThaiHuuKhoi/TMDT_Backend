package com.KhoiCG.TMDT.modules.order.mapper;

import com.KhoiCG.TMDT.modules.order.dto.OrderItemResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) return null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                // Format lại tiền tệ cho đẹp (Có thể dùng thư viện format số)
                .totalAmountFormatted(String.format("%,.0f VNĐ", order.getTotalAmount()))
                .shippingAddress(order.getShippingAddress())
                .items(order.getItems() != null ?
                        order.getItems().stream().map(this::toOrderItemResponse).collect(Collectors.toList())
                        : null)
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) return null;

        return OrderItemResponse.builder()
                .productName(item.getProductName())
                .sku(item.getSku())
                // Gắn thêm thông tin biến thể nếu cần
                .variantInfo(item.getVariant() != null ? "SKU: " + item.getVariant().getSku() : "")
                .quantity(item.getQuantity())
                .priceFormatted(String.format("%,.0f VNĐ", item.getPriceAtPurchase()))
                .build();
    }
}