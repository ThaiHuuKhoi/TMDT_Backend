package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.modules.auth.entity.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    // 1. API Gửi từ Frontend để lấy URL chuyển hướng sang VNPay
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(HttpServletRequest request) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        // Sinh một mã giao dịch ngẫu nhiên (hoặc dùng System.currentTimeMillis())
        String txnRef = "VNPay_" + UUID.randomUUID().toString().substring(0, 8);
        String ipAddress = request.getRemoteAddr();

        // Tận dụng luồng "Đóng băng giỏ hàng" cực an toàn đã viết trước đó
        Order pendingOrder = orderService.createPendingOrder(userId, txnRef);

        // Tạo URL VNPay với số tiền của đơn hàng PENDING
        String paymentUrl = vnPayService.createOrder(
                pendingOrder.getTotalAmount().longValue(),
                "Thanh toan don hang " + txnRef,
                txnRef,
                ipAddress
        );

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // 2. API Nhận IPN (Webhook) từ hệ thống VNPay gọi ngầm về Server
    @GetMapping("/ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("Nhận IPN từ VNPay: {}", params);

        if (vnPayService.verifySignature(params)) {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");

            if ("00".equals(vnp_ResponseCode)) {
                // Gọi hàm Idempotency đã viết trước đó để chốt đơn & trừ kho
                // (Vì hàm confirmOrderPayment nhận sessionId, ở đây ta truyền txnRef)
                orderService.confirmOrderPayment(txnRef);
                log.info("Xử lý IPN thành công cho giao dịch: {}", txnRef);

                // Trả về định dạng mà VNPay yêu cầu để xác nhận đã nhận IPN
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            } else {
                // Giao dịch lỗi/hủy
                // Có thể viết thêm logic đổi status order thành CANCELLED
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success (Failed Transaction)"));
            }
        }

        return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
    }
}