package com.KhoiCG.TMDT.modules.shipping.repository;

import com.KhoiCG.TMDT.modules.shipping.entity.ShippingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingLogRepository extends JpaRepository<ShippingLog, Long> {

    List<ShippingLog> findByOrderIdOrderByReportedAtDesc(Long orderId);
}