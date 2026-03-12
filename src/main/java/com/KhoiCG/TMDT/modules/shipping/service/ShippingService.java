package com.KhoiCG.TMDT.modules.shipping.service;

import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderStatus;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.shipping.dto.CreateShippingForOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingLogResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;
import com.KhoiCG.TMDT.modules.shipping.entity.ShippingLog;
import com.KhoiCG.TMDT.modules.shipping.repository.ShippingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ShippingService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ShippingManager shippingManager;
    private final OrderRepository orderRepository;
    private final ShippingLogRepository shippingLogRepository;

    public ShippingFeeResponse calculateFee(ShippingFeeRequest req) {
        double fee = shippingManager.calculateFee(
                req.getProviderCode(),
                req.getToWardCode(),
                String.valueOf(req.getToDistrictId()),
                req.getWeightInGrams()
        );
        return ShippingFeeResponse.builder()
                .providerCode(req.getProviderCode().toUpperCase())
                .fee(fee)
                .currency("VND")
                .build();
    }

    @Transactional
    public ShippingOrderResponse createShippingForOrder(Long orderId, CreateShippingForOrderRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        ShippingOrderRequest carrierReq = new ShippingOrderRequest();
        carrierReq.setCustomerName(req.getCustomerName());
        carrierReq.setCustomerPhone(req.getCustomerPhone());
        carrierReq.setAddress(req.getAddress());
        carrierReq.setToDistrictId(req.getToDistrictId());
        carrierReq.setToWardCode(req.getToWardCode());
        carrierReq.setWeightInGrams(req.getWeightInGrams());
        carrierReq.setCodAmount(req.getCodAmount());

        ShippingOrderResponse carrierResp = shippingManager.pushOrderToCarrier(req.getProviderCode(), carrierReq);

        order.setShippingProvider(req.getProviderCode().toUpperCase());
        order.setTrackingCode(carrierResp.getTrackingCode());
        order.setShippingFee(BigDecimal.valueOf(carrierResp.getShippingFee()));

        if (carrierResp.getExpectedDeliveryTime() != null && !carrierResp.getExpectedDeliveryTime().isBlank()) {
            // Keep it simple: store "now" if parse fails.
            try {
                order.setExpectedDeliveryDate(LocalDateTime.parse(carrierResp.getExpectedDeliveryTime()));
            } catch (Exception ignored) {
                order.setExpectedDeliveryDate(LocalDateTime.now().plusDays(3));
            }
        }

        // Mark order shipped when we have a tracking code.
        if (order.getTrackingCode() != null && !order.getTrackingCode().isBlank()) {
            order.setStatus(OrderStatus.SHIPPED);
        }

        Order saved = orderRepository.save(order);

        ShippingLog initLog = ShippingLog.builder()
                .order(saved)
                .status("created")
                .message("Đã tạo vận đơn " + saved.getTrackingCode() + " (" + saved.getShippingProvider() + ")")
                .reportedAt(LocalDateTime.now())
                .build();
        shippingLogRepository.save(initLog);

        return carrierResp;
    }

    public List<ShippingLogResponse> getLogsByOrderId(Long orderId) {
        return shippingLogRepository.findByOrderIdOrderByReportedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ShippingLogResponse toResponse(ShippingLog log) {
        return ShippingLogResponse.builder()
                .status(log.getStatus())
                .message(log.getMessage())
                .reportedAt(log.getReportedAt() != null ? log.getReportedAt().format(ISO_FORMATTER) : null)
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().format(ISO_FORMATTER) : null)
                .build();
    }
}

