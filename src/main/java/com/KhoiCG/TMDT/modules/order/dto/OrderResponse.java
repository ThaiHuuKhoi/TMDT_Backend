package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderDate;
    private String status;
    private String totalAmountFormatted;
    private String shippingAddress;
    private List<OrderItemResponse> items;
}