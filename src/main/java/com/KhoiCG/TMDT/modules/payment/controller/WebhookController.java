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

                String sessionId = sessionNode.path("id").asText();

                // Lấy userId mà chúng ta đã truyền vào từ StripeService
                String userIdStr = sessionNode.path("client_reference_id").asText();

                if (userIdStr == null || userIdStr.isEmpty() || "null".equals(userIdStr)) {
                    log.error("Cảnh báo: Không tìm thấy client_reference_id cho session {}", sessionId);
                    return ResponseEntity.ok().build(); // Bỏ qua giao dịch lỗi này
                }

                long amountTotal = sessionNode.path("amount_total").asLong(); // Lúc này amount_total chính là tiền VND chuẩn
                String email = sessionNode.path("customer_details").path("email").asText("unknown@mail.com");

                log.info("Payment Succeeded - Session: {} | User: {}", sessionId, userIdStr);

                PaymentSuccessEvent successEvent = new PaymentSuccessEvent();
                successEvent.setUserId(userIdStr);
                successEvent.setEmail(email);
                successEvent.setAmount(amountTotal);
                successEvent.setStatus("success");
                successEvent.setSessionId(sessionId);

                kafkaTemplate.send("payment.successful", successEvent);

            } catch (Exception e) {
                log.error("Error parsing JSON manually: ", e);
                return ResponseEntity.internalServerError().build();
            }
        }

        return ResponseEntity.ok(Map.of("received", true));
    }
}