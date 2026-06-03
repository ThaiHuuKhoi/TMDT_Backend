package com.KhoiCG.TMDT.modules.shipping.service;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingConfigRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingConfigResponse;
import com.KhoiCG.TMDT.modules.shipping.entity.ShippingConfig;
import com.KhoiCG.TMDT.modules.shipping.repository.ShippingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShippingConfigService {

    private static final Long CONFIG_ID = 1L;

    private final ShippingConfigRepository configRepository;

    @Value("${application.shipping.default-fee-vnd:20000}")
    private long defaultFeeVnd;

    @Value("${application.shipping.grams-per-item-unit:500}")
    private int gramsPerItemUnit;

    public ShippingConfig getConfig() {
        return configRepository.findById(CONFIG_ID).orElseGet(() ->
                configRepository.save(ShippingConfig.builder()
                        .id(CONFIG_ID)
                        .defaultFeeVnd(defaultFeeVnd)
                        .gramsPerItemUnit(gramsPerItemUnit)
                        .build())
        );
    }

    public ShippingConfigResponse getConfigResponse() {
        ShippingConfig cfg = getConfig();
        return ShippingConfigResponse.builder()
                .defaultFeeVnd(cfg.getDefaultFeeVnd())
                .gramsPerItemUnit(cfg.getGramsPerItemUnit())
                .build();
    }

    @Transactional
    public ShippingConfigResponse updateConfig(ShippingConfigRequest request) {
        ShippingConfig cfg = getConfig();
        cfg.setDefaultFeeVnd(request.getDefaultFeeVnd());
        cfg.setGramsPerItemUnit(request.getGramsPerItemUnit());
        configRepository.save(cfg);
        return ShippingConfigResponse.builder()
                .defaultFeeVnd(cfg.getDefaultFeeVnd())
                .gramsPerItemUnit(cfg.getGramsPerItemUnit())
                .build();
    }
}
