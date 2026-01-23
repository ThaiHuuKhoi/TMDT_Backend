package com.KhoiCG.TMDT.paymentService.controler;

import com.KhoiCG.TMDT.orderService.dto.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
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
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper; // 👇 Dùng Jackson để parse JSON thủ công

    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // 1. Vẫn verify chữ ký để bảo mật (Cái này Stripe làm tốt)
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid Signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Signature");
        } catch (Exception e) {
            log.error("❌ Webhook Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                // 2. Thay vì dùng event.getDataObjectDeserializer()... (hay bị null)
                // Ta dùng Jackson parse trực tiếp payload string để lấy data
                JsonNode rootNode = objectMapper.readTree(payload);
                JsonNode sessionNode = rootNode.path("data").path("object");

                // Lấy các trường cần thiết thủ công (An toàn tuyệt đối)
                String sessionId = sessionNode.path("id").asText();
                String userId = sessionNode.path("client_reference_id").asText("anonymous");
                long amountTotal = sessionNode.path("amount_total").asLong();

                String email = "unknown@mail.com";
                if (sessionNode.has("customer_details") && !sessionNode.path("customer_details").isNull()) {
                    email = sessionNode.path("customer_details").path("email").asText("unknown@mail.com");
                }

                log.info("✅ Payment Succeeded (Manual Parse) - Session: {}", sessionId);

                // 3. Tạo Event bắn Kafka
                PaymentSuccessEvent successEvent = new PaymentSuccessEvent();
                successEvent.setUserId(userId);
                successEvent.setEmail(email);
                successEvent.setAmount(amountTotal);
                successEvent.setStatus("success");

                log.info("🚀 Sending Kafka Event: {}", successEvent);
                kafkaTemplate.send("payment.successful", successEvent);

            } catch (Exception e) {
                log.error("🔥 Error parsing JSON manually: ", e);
                return ResponseEntity.internalServerError().build();
            }
        } else {
            log.info("ℹ️ Ignored event: {}", event.getType());
        }

        return ResponseEntity.ok(Map.of("received", true));
    }
}