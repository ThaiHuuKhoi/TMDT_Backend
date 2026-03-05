package com.KhoiCG.TMDT.modules.order.event;

import com.KhoiCG.TMDT.modules.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCompletedEvent {
    private final Order order;
}