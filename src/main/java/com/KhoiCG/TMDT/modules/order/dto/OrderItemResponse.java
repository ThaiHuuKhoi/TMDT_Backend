// File: src/main/java/com/KhoiCG/TMDT/modules/order/dto/OrderItemResponse.java
package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private String sku;
    private String variantInfo;
    private Integer quantity;
    private Long priceAtPurchase;
    private String priceFormatted;
    private String productImage; //
}