package com.KhoiCG.TMDT.modules.shipping.controller;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingConfigRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingConfigResponse;
import com.KhoiCG.TMDT.modules.shipping.service.ShippingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipping-config")
@RequiredArgsConstructor
public class ShippingConfigController {

    private final ShippingConfigService shippingConfigService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ShippingConfigResponse> getConfig() {
        return ResponseEntity.ok(shippingConfigService.getConfigResponse());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ShippingConfigResponse> updateConfig(@Valid @RequestBody ShippingConfigRequest request) {
        return ResponseEntity.ok(shippingConfigService.updateConfig(request));
    }
}
