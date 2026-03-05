package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Data;

@Data
public class PaymentSuccessEvent {
    private String userId;
    private String email;
    private Long amount;
    private String status;
}