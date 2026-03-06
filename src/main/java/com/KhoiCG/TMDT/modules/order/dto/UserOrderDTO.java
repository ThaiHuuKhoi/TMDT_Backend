package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserOrderDTO {
    private Long id;
    private String name;
    private String email;
}