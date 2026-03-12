package com.KhoiCG.TMDT.modules.shipping.provider;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(name = "ghtk.api.token")
public class GhtkShippingProvider implements ShippingProvider {

    @Value("${ghtk.api.token:}")
    private String apiToken;

    @Override
    public String getProviderCode() {
        return "GHTK";
    }

    @Override
    public ShippingOrderResponse createShippingOrder(ShippingOrderRequest request) {
        log.info("Đang gọi API tạo đơn sang Giao Hàng Tiết Kiệm (GHTK)...");

        // TODO: Implement real GHTK API call when needed.
        return ShippingOrderResponse.builder()
                .trackingCode("S1.GHTK." + System.currentTimeMillis())
                .shippingFee(30000.0)
                .expectedDeliveryTime(LocalDateTime.now().plusDays(3).toString())
                .build();
    }

    @Override
    public double calculateShippingFee(String toWardCode, String toDistrictCode, int weightInGrams) {
        return 30000.0; // Phí giả định
    }
}