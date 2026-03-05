package com.KhoiCG.TMDT.modules.email.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderCreatedEvent {
    private String email;
    private Long amount;
    private String status;
}