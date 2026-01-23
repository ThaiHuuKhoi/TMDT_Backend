package com.KhoiCG.TMDT.paymentService.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateSessionRequest {
    private List<CartItemDto> items;
    private String couponCode;
}