package com.KhoiCG.TMDT.modules.shipping.service;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingQuoteRequest;
import com.KhoiCG.TMDT.modules.shipping.entity.ShippingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShippingQuoteService {

	private final ShippingManager shippingManager;
	private final ShippingConfigService shippingConfigService;

	@Autowired
	public ShippingQuoteService(
			@Autowired(required = false) ShippingManager shippingManager,
			ShippingConfigService shippingConfigService) {
		this.shippingManager = shippingManager;
		this.shippingConfigService = shippingConfigService;
	}

	public ShippingFeeResponse quote(ShippingQuoteRequest req) {
		ShippingConfig cfg = shippingConfigService.getConfig();
		long defaultFeeVnd = cfg.getDefaultFeeVnd();
		int gramsPerItemUnit = cfg.getGramsPerItemUnit();

		int units = req.getItemCount() == null || req.getItemCount() < 1 ? 1 : req.getItemCount();
		int weightGrams = Math.max(gramsPerItemUnit, units * gramsPerItemUnit);

		boolean tryGhn = shippingManager != null
				&& req.getProviderCode() != null
				&& "GHN".equalsIgnoreCase(req.getProviderCode().trim())
				&& req.getToDistrictId() != null
				&& req.getToWardCode() != null
				&& !req.getToWardCode().isBlank();

		if (tryGhn) {
			try {
				double fee = shippingManager.calculateFee(
						"GHN",
						req.getToWardCode().trim(),
						String.valueOf(req.getToDistrictId()),
						weightGrams
				);
				if (fee > 0) {
					return ShippingFeeResponse.builder()
							.providerCode("GHN")
							.fee(fee)
							.currency("VND")
							.source("CARRIER")
							.build();
				}
			} catch (Exception e) {
				log.warn("Shipping carrier quote failed, using flat fee: {}", e.getMessage());
			}
		}

		return ShippingFeeResponse.builder()
				.providerCode("DEFAULT")
				.fee(defaultFeeVnd)
				.currency("VND")
				.source("FLAT")
				.build();
	}

	public long computeFeeVndForItemCount(int itemCount) {
		ShippingQuoteRequest r = new ShippingQuoteRequest();
		r.setItemCount(Math.max(1, itemCount));
		return Math.round(quote(r).getFee());
	}
}
