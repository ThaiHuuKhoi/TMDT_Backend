// File: src/main/java/com/KhoiCG/TMDT/modules/order/dto/OrderResponse.java
package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private UserOrderDTO user;
    private String createdAt;
    private String status;
    private BigDecimal totalAmount;
    private String totalAmountFormatted;
    private String shippingAddress;
    private String stripeSessionId;
    private List<OrderItemResponse> items;
}