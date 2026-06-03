package com.KhoiCG.TMDT.modules.order.mapper;

import com.KhoiCG.TMDT.modules.order.dto.CartItemSummaryResponse;
import com.KhoiCG.TMDT.modules.order.dto.CartSummaryResponse;
import com.KhoiCG.TMDT.modules.order.entity.Cart;
import com.KhoiCG.TMDT.modules.order.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CartMapper {
    public CartSummaryResponse toCartSummary(Cart cart) {
        BigDecimal totalAmount = cart.getItems().stream()
                .map(i -> i.getVariant().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        return CartSummaryResponse.builder()
                .id(cart.getId())
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .items(cart.getItems().stream().map(this::toCartItemSummary).toList())
                .build();
    }

    public CartItemSummaryResponse toCartItemSummary(CartItem item) {
        BigDecimal unitPrice = item.getVariant().getPrice();
        return CartItemSummaryResponse.builder()
                .variantId(item.getVariant().getId())
                .sku(item.getVariant().getSku())
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
