package com.KhoiCG.TMDT.modules.shipping.service;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;
import com.KhoiCG.TMDT.modules.shipping.provider.ShippingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ShippingManager {

    // Chứa danh sách các đối tác (Key: "GHN" hoặc "GHTK", Value: Class thực thi)
    private final Map<String, ShippingProvider> providers;

    @Autowired
    public ShippingManager(List<ShippingProvider> providerList) {
        // Spring Boot tự động tìm mọi class implements ShippingProvider và nhét vào Map này
        this.providers = providerList.stream()
                .collect(Collectors.toMap(ShippingProvider::getProviderCode, Function.identity()));
    }

    // Hàm gọi từ OrderService
    public ShippingOrderResponse pushOrderToCarrier(String providerCode, ShippingOrderRequest request) {
        ShippingProvider provider = providers.get(providerCode.toUpperCase());

        if (provider == null) {
            throw new RuntimeException("Hệ thống chưa hỗ trợ đơn vị vận chuyển: " + providerCode);
        }

        // Tự động gọi đúng code của GHN hoặc GHTK dựa trên providerCode
        return provider.createShippingOrder(request);
    }

    public double calculateFee(String providerCode, String toWardCode, String toDistrictCode, int weightInGrams) {
        ShippingProvider provider = providers.get(providerCode.toUpperCase());
        if (provider == null) {
            throw new RuntimeException("Hệ thống chưa hỗ trợ đơn vị vận chuyển: " + providerCode);
        }
        return provider.calculateShippingFee(toWardCode, toDistrictCode, weightInGrams);
    }
}