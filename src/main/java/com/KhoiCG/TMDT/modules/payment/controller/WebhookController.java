package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.modules.order.dto.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                JsonNode rootNode = objectMapper.readTree(payload);
                JsonNode sessionNode = rootNode.path("data").path("object");

                String stripeOriginalSessionId = sessionNode.path("id").asText();

                // Lấy txnRef mà chúng ta đã gửi đi lúc nãy
                String txnRef = sessionNode.path("client_reference_id").asText();

                if (txnRef == null || txnRef.isEmpty() || "null".equals(txnRef)) {
                    log.error("Cảnh báo: Không tìm thấy client_reference_id cho session {}", stripeOriginalSessionId);
                    return ResponseEntity.ok().build();
                }

                long amountTotal = sessionNode.path("amount_total").asLong();
                String email = sessionNode.path("customer_details").path("email").asText("unknown@mail.com");

                log.info("Payment Succeeded - TxnRef: {}", txnRef);

                PaymentSuccessEvent successEvent = new PaymentSuccessEvent();
                // Vì OrderService đang mong đợi sessionId (để tìm kiếm), ta nhét txnRef vào đây
                successEvent.setSessionId(txnRef);
                // Cập nhật các trường còn lại (Bạn có thể bỏ trường userId đi nếu muốn vì Kafka event chỉ cần sessionId/txnRef để confirm đơn)
                successEvent.setEmail(email);
                successEvent.setAmount(amountTotal);
                successEvent.setStatus("success");

                // Đẩy thông báo lên Kafka -> OrderListener sẽ bắt lấy txnRef và gọi confirmOrderPayment(txnRef)
                kafkaTemplate.send("payment.successful", successEvent);

            } catch (Exception e) {
                log.error("Error parsing JSON manually: ", e);
                return ResponseEntity.internalServerError().build();
            }
        }

        return ResponseEntity.ok(Map.of("received", true));
    }
}