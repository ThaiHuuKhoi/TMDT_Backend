package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Data;

/**
 * Ước tính phí ship. Chỉ cần itemCount để dùng mức phí cố định / khối lượng mặc định.
 * Thêm provider + quận/xã GHN khi muốn tính qua đối tác (cần {@code shipping.enabled=true}).
 */
@Data
public class ShippingQuoteRequest {
	/** Tổng số lượng hàng trong giỏ (sum quantity). */
	private Integer itemCount;
	private String providerCode;
	private Integer toDistrictId;
	private String toWardCode;
}
