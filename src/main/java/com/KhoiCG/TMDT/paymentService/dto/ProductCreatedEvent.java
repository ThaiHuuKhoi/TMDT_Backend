package com.KhoiCG.TMDT.paymentService.dto;

import lombok.Data;

@Data
public class ProductCreatedEvent {
    private String id;
    private String name;
    private Long price; // Java dùng Long thay vì number
}