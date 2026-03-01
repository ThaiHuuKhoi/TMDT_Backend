package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Data;

// modules/order/dto/PaymentSuccessEvent.java
@Data
public class PaymentSuccessEvent {
    private String userId;
    private String email;
    private Long amount;
    private String status;
    // các field khác...
}