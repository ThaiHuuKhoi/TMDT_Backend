package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.*;
import com.KhoiCG.TMDT.modules.order.entity.*;
import com.KhoiCG.TMDT.modules.order.event.OrderCompletedEvent;
import com.KhoiCG.TMDT.modules.order.mapper.OrderMapper;
import com.KhoiCG.TMDT.modules.order.repository.CouponRepository;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.repository.PaymentRepository;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.service.InventoryService;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepo userRepo;
    private final StripeService stripeService;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    @Transactional
    public void createOrder(PaymentSuccessEvent event) {
        try {
            User user = userRepo.findById(Long.valueOf(event.getUserId()))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Order order = Order.builder()
                    .user(user)
                    .totalAmount(BigDecimal.valueOf(event.getAmount()))
                    .status(OrderStatus.valueOf(event.getStatus().toUpperCase()))
                    .statusHistories(new ArrayList<>())
                    .build();

            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .status(order.getStatus())
                    .note("Tạo đơn hàng từ hệ thống nội bộ")
                    .build();
            order.getStatusHistories().add(history);

            Order savedOrder = orderRepository.save(order);

            OrderCreatedEvent outEvent = new OrderCreatedEvent(
                    user.getEmail(), savedOrder.getTotalAmount().longValue(), savedOrder.getStatus().name());

            kafkaTemplate.send("order.created", outEvent);
            log.info("Order created and event sent for user: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Error creating order from Kafka", e);
            throw e;
        }
    }

    // --- 2. Lấy danh sách Order ---
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }
    public List<Order> getAllOrders(int limit) {
        return orderRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    public OrderResponse getOrderDetails(Long id, Long userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));
        return orderMapper.toOrderResponse(order);
    }

    // --- 3. Biểu đồ thống kê ---
    public List<OrderChartResponse> getOrderChart() {
        List<Object[]> rawStats = orderRepository.getRawMonthlyStats(LocalDateTime.now().minusMonths(6));
        List<OrderChartResponse> responseList = new ArrayList<>();

        for (Object[] row : rawStats) {
            String month = (String) row[0];

            Number totalNum = (Number) row[1];
            Number successfulNum = (Number) row[2];

            Long total = totalNum != null ? totalNum.longValue() : 0L;
            Long successful = successfulNum != null ? successfulNum.longValue() : 0L;

            responseList.add(new OrderChartResponse(month, total, successful));
        }

        return responseList;
    }

    // --- 4. Cập nhật trạng thái (Dành cho Admin) ---
    @Transactional
    public Order updateOrderStatus(Long id, String newStatusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        order.setStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .note("Quản trị viên cập nhật trạng thái")
                .build();
        order.getStatusHistories().add(history);

        return orderRepository.save(order);
    }

    // 1. Tạo đơn hàng PENDING (Snapshot giỏ hàng) trước khi thanh toán
    @Transactional
    public Order createPendingOrder(Long userId, String stripeSessionId, String couponCode) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể tạo đơn hàng!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Khởi tạo Order trạng thái PENDING
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .stripeSessionId(stripeSessionId)
                .build();

        // Copy từ CartItem sang OrderItem (Khóa chết giá trị)
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

            // Chỉ check xem kho còn không, CHƯA TRỪ KHO vội
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + "' không đủ số lượng!");
            }

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal); // Cộng dồn tiền hàng (Chưa giảm giá)

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productId(variant.getProduct().getId())
                    .productName(variant.getProduct().getName())
                    .sku(variant.getSku())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(variant.getPrice())
                    .build();
            order.addOrderItem(orderItem);
        }

        // --- BỔ SUNG LOGIC XỬ LÝ MÃ GIẢM GIÁ Ở ĐÂY ---
        if (couponCode != null && !couponCode.isBlank()) {
            CouponCheckRequest checkReq = new CouponCheckRequest();
            checkReq.setCode(couponCode);
            checkReq.setOrderAmount(totalAmount.longValue());

            CouponResponse couponCheck = couponService.applyCoupon(checkReq);

            if (couponCheck.isValid()) {
                Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase()).orElse(null);

                order.setCoupon(coupon);
                order.setDiscountAmount(BigDecimal.valueOf(couponCheck.getDiscountAmount()));

                totalAmount = BigDecimal.valueOf(couponCheck.getFinalPrice());
            } else {
                throw new RuntimeException("Mã giảm giá không hợp lệ: " + couponCheck.getMessage());
            }
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrderPayment(String sessionId, Payment.PaymentMethod paymentMethod) {

        //  1. IDEMPOTENCY CHECK: Kẻ gác cổng
        if (paymentRepository.findByTransactionId(sessionId).isPresent()) {
            log.info("Giao dịch {} đã được xử lý trước đó. Bỏ qua để chống trùng lặp.", sessionId);
            return orderRepository.findByStripeSessionId(sessionId).orElse(null);
        }

        // 2. Tìm đơn hàng PENDING đã đóng băng ở bước trước
        Order order = orderRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch này!"));

        // Double check trạng thái Order (Phòng hờ race-condition mili-giây)
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return order;
        }

        try {
            inventoryService.deductInventoryForOrder(order.getItems());

            if (order.getCoupon() != null) {
                Coupon coupon = order.getCoupon();
                coupon.setUsedCount(coupon.getUsedCount() + 1);
                couponRepository.save(coupon);
            }

            // 4. Cập nhật trạng thái Order
            order.setStatus(OrderStatus.COMPLETED);
            order.addStatusHistory(OrderStatusHistory.builder()
                    .status(OrderStatus.COMPLETED)
                    .note("Thanh toán Stripe thành công.")
                    .build());

            Order savedOrder = orderRepository.save(order);

            //  5. LƯU VẾT THANH TOÁN (Khóa giao dịch)
            // Lần sau webhook/frontend có gọi lại sessionId này thì sẽ bị chặn ở bước 1
            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .paymentMethod(paymentMethod)
                    .transactionId(sessionId)
                    .amount(savedOrder.getTotalAmount())
                    .status(Payment.PaymentStatus.SUCCESS)
                    .build();
            paymentRepository.save(payment);

            // 6. Phát sự kiện (Xóa giỏ hàng, gửi Email, v.v...)
            eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder));

            return savedOrder;

        } catch (Exception e) {
            log.error("Lỗi khi hoàn tất đơn hàng: ", e);

            Payment failedPayment = Payment.builder()
                    .order(order)
                    .paymentMethod(Payment.PaymentMethod.STRIPE)
                    .transactionId(sessionId)
                    .amount(order.getTotalAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
            paymentRepository.save(failedPayment);

            throw new RuntimeException("Xử lý thanh toán thất bại: " + e.getMessage());
        }
    }
}