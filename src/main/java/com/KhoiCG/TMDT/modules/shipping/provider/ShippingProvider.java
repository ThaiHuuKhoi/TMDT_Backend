package com.KhoiCG.TMDT.modules.shipping.provider;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;

public interface ShippingProvider {

    String getProviderCode();

    ShippingOrderResponse createShippingOrder(ShippingOrderRequest request);

    double calculateShippingFee(String toWardCode, String toDistrictCode, int weightInGrams);
}