package com.KhoiCG.TMDT.emailService.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderCreatedEvent {
    private String email;
    private Long amount; // Lưu ý: Nodejs là number, Java nên dùng Long cho tiền
    private String status;
}