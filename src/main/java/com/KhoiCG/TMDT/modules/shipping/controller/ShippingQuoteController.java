package com.KhoiCG.TMDT.modules.shipping.controller;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingQuoteRequest;
import com.KhoiCG.TMDT.modules.shipping.service.ShippingQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingQuoteController {

	private final ShippingQuoteService shippingQuoteService;

	@PostMapping("/quote")
	public ResponseEntity<ShippingFeeResponse> quote(@RequestBody(required = false) ShippingQuoteRequest request) {
		if (request == null) {
			request = new ShippingQuoteRequest();
		}
		return ResponseEntity.ok(shippingQuoteService.quote(request));
	}
}
