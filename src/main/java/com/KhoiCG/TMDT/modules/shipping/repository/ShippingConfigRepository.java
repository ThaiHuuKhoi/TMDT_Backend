package com.KhoiCG.TMDT.modules.shipping.repository;

import com.KhoiCG.TMDT.modules.shipping.entity.ShippingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingConfigRepository extends JpaRepository<ShippingConfig, Long> {
}
