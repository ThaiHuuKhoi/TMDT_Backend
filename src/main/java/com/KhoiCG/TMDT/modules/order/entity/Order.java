package com.KhoiCG.TMDT.modules.order.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    private String userId;
    private String email;
    private Long amount;
    private String status;
    private String stripeSessionId;

    @CreatedDate
    private LocalDateTime createdAt;
}
