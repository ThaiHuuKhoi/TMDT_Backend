package com.KhoiCG.TMDT.modules.payment.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateSessionRequest {
    private List<CartItemDto> items;
    private String couponCode;
}