// File: src/main/java/com/KhoiCG/TMDT/modules/order/dto/OrderResponse.java
package com.KhoiCG.TMDT.modules.order.dto;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingLogResponse;
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
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private List<OrderItemResponse> items;
    private List<ShippingLogResponse> shippingLogs;
}