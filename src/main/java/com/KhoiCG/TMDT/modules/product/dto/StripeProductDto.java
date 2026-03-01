package com.KhoiCG.TMDT.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StripeProductDto {
    private String id;
    private String name;
    private Long price;
}
