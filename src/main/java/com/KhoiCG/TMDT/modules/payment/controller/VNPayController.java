package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.dto.CreateSessionRequest;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@Valid @RequestBody CreateSessionRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId();

        String txnRef = "VNPay_" + UUID.randomUUID().toString().substring(0, 8);
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ipAddress = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        Order pendingOrder = orderService.createPendingOrder(
                userId,
                txnRef,
                request.getCouponCode(),
                request.getShipping()
        );

        String paymentUrl = vnPayService.createOrder(
                pendingOrder.getTotalAmount().longValue(),
                "Thanh toan don hang " + txnRef,
                txnRef,
                ipAddress
        );

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    /**
     * IPN: VNPay server gọi. Phản hồi theo định dạng RspCode/Message.
     */
    @GetMapping("/ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN: {}", params.keySet());
        return processIpnForGateway(params);
    }

    /**
     * Return URL: trình duyệt user sau khi thanh toán (chữ ký VNPay vẫn được verify).
     * Không dùng cùng payload/semantics với IPN server-to-server; tách endpoint để rõ ràng và an toàn cấu hình.
     */
    @GetMapping("/return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("VNPay return (browser): vnp_ResponseCode={}, vnp_TxnRef present={}",
                params.get("vnp_ResponseCode"), params.containsKey("vnp_TxnRef"));
        return processReturnForBrowser(params);
    }

    private ResponseEntity<?> processIpnForGateway(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Invalid Transaction Reference"));
        }

        if ("00".equals(vnp_ResponseCode)) {
            orderService.confirmOrderPayment(txnRef, Payment.PaymentMethod.VNPAY);
            log.info("IPN xác nhận thành công: {}", txnRef);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        }
        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success (Failed Transaction)"));
    }

    private ResponseEntity<?> processReturnForBrowser(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "ok", false,
                            "code", "INVALID_SIGNATURE",
                            "message", "Chữ ký giao dịch không hợp lệ"
                    ));
        }
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("ok", false, "code", "MISSING_TXN_REF", "message", "Thiếu mã giao dịch"));
        }

        if ("00".equals(vnp_ResponseCode)) {
            orderService.confirmOrderPayment(txnRef, Payment.PaymentMethod.VNPAY);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "vnp_ResponseCode", "00",
                    "txnRef", txnRef
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", false,
                "vnp_ResponseCode", vnp_ResponseCode != null ? vnp_ResponseCode : "",
                "txnRef", txnRef,
                "message", "Giao dịch chưa thành công hoặc đã hủy"
        ));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập");
        }
        return userPrincipal.getUser().getId();
    }
}
