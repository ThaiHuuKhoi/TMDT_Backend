package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductVariantRepository variantRepository;

    @Transactional
    public void deductInventoryForOrder(List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + item.getProductName() + "' vừa hết hàng. Vui lòng liên hệ CSKH!");
            }
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepository.save(variant);
        }
    }
}