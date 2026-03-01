package com.KhoiCG.TMDT.modules.payment.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private String id;
    private String name;
    private int quantity;
    private Double price;
    private String image;
}