package com.KhoiCG.TMDT.modules.order.dto;
import lombok.Data;

@Data
public class CartRequest {
    private Long variantId;
    private Integer quantity;
}