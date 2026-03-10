package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.dto.CreateSessionRequest;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<?> createPayment(@RequestBody CreateSessionRequest request, HttpServletRequest httpRequest) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        String txnRef = "VNPay_" + UUID.randomUUID().toString().substring(0, 8);
        String ipAddress = httpRequest.getRemoteAddr();

        Order pendingOrder = orderService.createPendingOrder(userId, txnRef, request.getCouponCode());

        String paymentUrl = vnPayService.createOrder(
                pendingOrder.getTotalAmount().longValue(),
                "Thanh toan don hang " + txnRef,
                txnRef,
                ipAddress
        );

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    @GetMapping("/ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("Nhận IPN từ VNPay: {}", params);

        if (vnPayService.verifySignature(params)) {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");

            if ("00".equals(vnp_ResponseCode)) {
                orderService.confirmOrderPayment(txnRef, Payment.PaymentMethod.VNPAY);
                log.info("Xử lý IPN thành công cho giao dịch: {}", txnRef);

                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            } else {
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success (Failed Transaction)"));
            }
        }

        return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
    }
}