package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Data;

@Data
public class ShippingOrderRequest {
    private String customerName;
    private String customerPhone;
    private String address;

    // GHN và GHTK đều cần mã ID Tỉnh/Huyện/Xã để tính đúng tuyến
    private Integer toDistrictId;
    private String toWardCode;

    private int weightInGrams;
    private long codAmount; // Số tiền thu hộ
}