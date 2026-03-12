package com.KhoiCG.TMDT.modules.shipping.controller;

import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderStatus;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.shipping.entity.ShippingLog;
import com.KhoiCG.TMDT.modules.shipping.repository.ShippingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/shipping")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
public class WebhookShippingController {

    private final OrderRepository orderRepository;
    private final ShippingLogRepository shippingLogRepository;

    /**
     * API này cung cấp cho GHN. Bạn phải lên trang web của GHN, dán URL này vào phần cấu hình Webhook:
     * Ví dụ: https://ten-mien-cua-ban.com/api/webhooks/shipping/ghn
     */
    @PostMapping("/ghn")
    @Transactional
    public ResponseEntity<String> handleGhnWebhook(@RequestBody Map<String, Object> payload) {
        log.info("📥 Nhận được Webhook từ GHN: {}", payload);

        try {
            // 1. GHN sẽ gửi mã vận đơn (OrderCode) và trạng thái (Status)
            String trackingCode = payload.get("OrderCode") != null ? String.valueOf(payload.get("OrderCode")) : null;
            String ghnStatus = payload.get("Status") != null ? String.valueOf(payload.get("Status")) : null;
            String reason = payload.get("Reason") != null ? String.valueOf(payload.get("Reason")) : null; // Lý do (nếu có lỗi/hủy)

            if (trackingCode == null || trackingCode.isBlank() || ghnStatus == null || ghnStatus.isBlank()) {
                throw new IllegalArgumentException("Payload webhook thiếu OrderCode/Status");
            }

            // 2. Tìm đơn hàng trong DB của bạn bằng trackingCode
            Order order = orderRepository.findByTrackingCode(trackingCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với mã vận đơn: " + trackingCode));

            // 3. Tạo một dòng Log mới (Hiển thị lên Timeline ở Frontend)
            String message = translateGhnStatus(ghnStatus, reason);

            ShippingLog newLog = ShippingLog.builder()
                    .order(order)
                    .status(ghnStatus)
                    .message(message)
                    .reportedAt(LocalDateTime.now())
                    .build();

            shippingLogRepository.save(newLog);

            // 4. (Tùy chọn) Cập nhật trạng thái tổng của Order
            if ("delivered".equalsIgnoreCase(ghnStatus)) {
                order.setStatus(OrderStatus.COMPLETED); // Giao thành công -> Hoàn thành đơn
                orderRepository.save(order);
            } else if ("cancel".equalsIgnoreCase(ghnStatus) || "return".equalsIgnoreCase(ghnStatus)) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
            }

            // Phản hồi 200 OK để GHN biết bạn đã nhận được tin, nếu không họ sẽ gửi lại liên tục
            return ResponseEntity.ok("Thành công");

        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý Webhook GHN: ", e);
            // Trả về 400 để GHN biết có lỗi
            return ResponseEntity.badRequest().body("Lỗi xử lý webhook");
        }
    }

    // Hàm phụ trợ dịch tiếng Anh của GHN sang tiếng Việt cho user đọc
    private String translateGhnStatus(String ghnStatus, String reason) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick" -> "Người bán đang chuẩn bị hàng";
            case "picking" -> "Shipper đang trên đường đến lấy hàng";
            case "picked" -> "Đã lấy hàng thành công";
            case "delivering" -> "Đơn hàng đang được giao đến bạn";
            case "delivered" -> "Giao hàng thành công";
            case "cancel" -> "Đơn hàng đã bị hủy. Lý do: " + (reason != null ? reason : "Không rõ");
            case "return" -> "Giao hàng thất bại, đang hoàn trả về shop";
            default -> "Trạng thái vận chuyển: " + ghnStatus; // Fallback
        };
    }
}