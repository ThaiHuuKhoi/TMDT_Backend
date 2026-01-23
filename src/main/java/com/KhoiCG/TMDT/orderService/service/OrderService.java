package com.KhoiCG.TMDT.orderService.service;

import com.KhoiCG.TMDT.orderService.dto.OrderChartResponse;
import com.KhoiCG.TMDT.orderService.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.orderService.dto.PaymentSuccessEvent;
import com.KhoiCG.TMDT.orderService.entity.Order;
import com.KhoiCG.TMDT.orderService.repository.OrderRepository;
import com.KhoiCG.TMDT.paymentService.service.StripeService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplate mongoTemplate; // Dùng cho Aggregation phức tạp
    private final StripeService stripeService;

    public Order createOrderFromStripe(String sessionId) {
        // A. Kiểm tra xem đơn này đã tạo chưa
        if (orderRepository.existsByStripeSessionId(sessionId)) {
            log.info("Order already exists for session: {}", sessionId);
            throw new RuntimeException("Order already exists");
        }

        try {
            // B. Verify với Stripe
            Session session = stripeService.retrieveSession(sessionId);

            if (!"paid".equals(session.getPaymentStatus())) {
                throw new RuntimeException("Payment not completed yet");
            }

            // C. Lấy thông tin User
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : "unknown@mail.com";

            // D. Tạo Order và Lưu vào DB
            Order order = new Order();
            order.setUserId(userId);
            order.setEmail(email);
            order.setAmount(session.getAmountTotal());
            order.setStatus("COMPLETED");
            order.setStripeSessionId(sessionId);
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);
            log.info("✅ ORDER SAVED TO MONGODB: {}", savedOrder.getId()); // Log 1

            // ======================================================
            // E. GỬI SỰ KIỆN KAFKA (ĐOẠN NÀY BẠN BỊ THIẾU)
            // ======================================================
            OrderCreatedEvent outEvent = new OrderCreatedEvent();
            outEvent.setEmail(savedOrder.getEmail());
            outEvent.setAmount(savedOrder.getAmount());
            outEvent.setStatus(savedOrder.getStatus());

            try {
                log.info("🚀 PREPARING TO SEND KAFKA EVENT for User: {}", savedOrder.getEmail()); // Log 2

                // Gửi tin nhắn
                kafkaTemplate.send("order.created", outEvent).get(); // .get() để đợi gửi xong (Debug)

                log.info("🎉 KAFKA EVENT SENT SUCCESSFULLY!"); // Log 3
            } catch (Exception e) {
                log.error("❌ KAFKA SEND FAILED: ", e);
            }
            // ======================================================

            return savedOrder;

        } catch (Exception e) {
            log.error("Error creating order from stripe", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    // --- 1. Xử lý tạo Order từ Kafka ---
    public void createOrder(PaymentSuccessEvent event) {
        try {
            Order order = new Order();
            order.setUserId(event.getUserId());
            order.setEmail(event.getEmail());
            order.setAmount(event.getAmount());
            order.setStatus(event.getStatus());
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);

            // Gửi sự kiện order.created (để gửi mail)
            OrderCreatedEvent outEvent = new OrderCreatedEvent();
            outEvent.setEmail(savedOrder.getEmail());
            outEvent.setAmount(savedOrder.getAmount());
            outEvent.setStatus(savedOrder.getStatus());

            kafkaTemplate.send("order.created", outEvent);
            log.info("Order created and event sent for user: {}", savedOrder.getEmail());

        } catch (Exception e) {
            log.error("Error creating order", e);
            throw e;
        }
    }

    // --- 2. Lấy danh sách Order ---
    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders(int limit) {
        // Mẹo: Dùng PageRequest để limit và sort
        // PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Ở đây mình dùng logic đơn giản tương đương code cũ
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().limit(limit).collect(Collectors.toList());
    }

    // --- 3. Biểu đồ thống kê (Phần khó nhất) ---
    // ... (Phần trên giữ nguyên)

    // --- 3. Biểu đồ thống kê (Phần khó nhất) ---
    public List<OrderChartResponse> getOrderChart() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy 6 tháng gần nhất (tính cả tháng hiện tại)
        LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("createdAt").gte(sixMonthsAgo)),
                Aggregation.project()
                        .andExpression("year(createdAt)").as("year")
                        .andExpression("month(createdAt)").as("month")
                        .and("status").as("status"),
                Aggregation.group("year", "month")
                        .count().as("total")
                        .sum(
                                org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                        .when(Criteria.where("status").is("COMPLETED")) // Lưu ý: check đúng status string (COMPLETED/success?)
                                        .then(1).otherwise(0)
                        ).as("successful"),
                Aggregation.sort(Sort.Direction.ASC, "year", "month")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "orders", Map.class);
        List<Map> rawData = results.getMappedResults();

        // Xử lý dữ liệu an toàn hơn
        List<OrderChartResponse> finalResult = new ArrayList<>();
        String[] monthNames = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 5; i >= 0; i--) {
            LocalDateTime d = now.minusMonths(i);
            int year = d.getYear();
            int month = d.getMonthValue();

            // Tìm trong rawData một cách an toàn
            Map match = rawData.stream().filter(m -> {
                // MongoDB trả về số có thể là Integer hoặc Long, nên dùng Number.intValue()
                int mYear = ((Number) m.get("year")).intValue();
                int mMonth = ((Number) m.get("month")).intValue();
                return mYear == year && mMonth == month;
            }).findFirst().orElse(null);

            long total = match != null ? ((Number) match.get("total")).longValue() : 0;
            long successful = match != null ? ((Number) match.get("successful")).longValue() : 0;

            finalResult.add(new OrderChartResponse(monthNames[month], total, successful));
        }

        return finalResult;
    }
}