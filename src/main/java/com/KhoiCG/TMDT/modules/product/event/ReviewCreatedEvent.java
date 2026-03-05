package com.KhoiCG.TMDT.modules.product.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewCreatedEvent {
    private final Long productId;
}