package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    private List<CartItemDto> items;
    private Object shippingForm;
    private String couponCode;
}