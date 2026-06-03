package com.KhoiCG.TMDT.modules.order.mapper;

import com.KhoiCG.TMDT.modules.order.dto.OrderItemResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.dto.UserOrderDTO;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingLogResponse;
import com.KhoiCG.TMDT.modules.shipping.repository.ShippingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ShippingLogRepository shippingLogRepository;

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) return null;

        UserOrderDTO userDto = UserOrderDTO.builder()
                .id(order.getUser().getId())
                .name(order.getUser().getName())
                .email(order.getUser().getEmail())
                .build();

        List<ShippingLogResponse> shippingLogs = order.getId() != null
                ? shippingLogRepository.findByOrderIdOrderByReportedAtDesc(order.getId())
                        .stream()
                        .map(log -> ShippingLogResponse.builder()
                                .status(log.getStatus())
                                .message(log.getMessage())
                                .reportedAt(log.getReportedAt() != null ? log.getReportedAt().format(ISO_FORMATTER) : null)
                                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().format(ISO_FORMATTER) : null)
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return OrderResponse.builder()
                .id(order.getId())
                .user(userDto)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(ISO_FORMATTER) : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .totalAmount(order.getTotalAmount())
                .totalAmountFormatted(String.format("%,.0f VNĐ", order.getTotalAmount()))
                .shippingAddress(order.getShippingAddress())
                .stripeSessionId(order.getStripeSessionId())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .items(order.getItems() != null ?
                        order.getItems().stream().map(this::toOrderItemResponse).collect(Collectors.toList())
                        : null)
                .shippingLogs(shippingLogs)
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) return null;

        String imageUrl = "/product-placeholder.png";
        if (item.getVariant() != null &&
                item.getVariant().getProduct() != null &&
                item.getVariant().getProduct().getImages() != null &&
                !item.getVariant().getProduct().getImages().isEmpty()) {
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
                .productImage(imageUrl)
                .build();
    }
}
