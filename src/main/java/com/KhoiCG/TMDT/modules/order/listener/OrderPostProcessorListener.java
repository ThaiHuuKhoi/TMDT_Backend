package com.KhoiCG.TMDT.modules.order.listener;

import com.KhoiCG.TMDT.modules.order.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.order.event.OrderCompletedEvent;
import com.KhoiCG.TMDT.modules.order.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessorListener {

    private final CartService cartService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Long userId = event.getOrder().getUser().getId();

        try {
            cartService.clearCart(userId);

            OrderCreatedEvent kafkaEvent = new OrderCreatedEvent(
                    event.getOrder().getUser().getEmail(),
                    event.getOrder().getTotalAmount().longValue(),
                    "COMPLETED"
            );
            kafkaTemplate.send("order.created", kafkaEvent);

            log.info("Đã xử lý xong các tác vụ hậu cần cho đơn hàng của user: {}", userId);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý hậu cần cho đơn hàng: ", e);
        }
    }
}