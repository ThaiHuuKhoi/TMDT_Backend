package com.KhoiCG.TMDT.modules.payment.dto;

import lombok.Data;

@Data
public class ProductCreatedEvent {
    private String id;
    private String name;
    private Long price; // Java dùng Long thay vì number
}