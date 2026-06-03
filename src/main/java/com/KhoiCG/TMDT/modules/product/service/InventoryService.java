package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.order.entity.OrderItem;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductVariantRepository variantRepository;

    @Transactional
    public void releaseInventoryForOrder(List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            try {
                variantRepository.save(variant);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                log.warn("optimistic lock on release variantId={}, retrying is not critical — inventory will be corrected manually", variant.getId());
            }
        }
    }

    @Transactional
    public void deductInventoryForOrder(List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "INSUFFICIENT_STOCK",
                        "Sản phẩm '" + item.getProductName() + "' vừa hết hàng. Vui lòng liên hệ CSKH!"
                );
            }
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            try {
                variantRepository.save(variant);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                log.warn("inventory optimistic lock conflict variantId={} productName={}", variant.getId(), item.getProductName());
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "INVENTORY_CONFLICT",
                        "Tồn kho sản phẩm vừa thay đổi, vui lòng thử đặt hàng lại."
                );
            }
        }
    }
}