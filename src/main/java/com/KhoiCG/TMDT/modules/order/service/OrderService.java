// File: src/main/java/com/KhoiCG/TMDT/modules/order/service/OrderService.java
package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.OrderChartResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderCreatedEvent;
import com.KhoiCG.TMDT.modules.order.dto.PaymentSuccessEvent;
import com.KhoiCG.TMDT.modules.order.entity.*;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.payment.dto.CartItemDto;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;

    @Transactional
    public Order createOrderFromStripe(String sessionId) {
        // 1. Tìm lại đơn hàng PENDING đã lưu trước đó
        Order order = orderRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch này trong hệ thống!"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng này đã được xử lý!");
        }

        try {
            // 2. Xác minh trạng thái với Stripe
            Session session = stripeService.retrieveSession(sessionId);
            if (!"paid".equals(session.getPaymentStatus())) {
                throw new RuntimeException("Thanh toán chưa hoàn tất trên Stripe");
            }

            // 🔥 BẢO MẬT 2 LỚP: Chống gian lận sửa giá trên frontend/Stripe
            BigDecimal stripeTotal = BigDecimal.valueOf(session.getAmountTotal() / 100.0);
            if (order.getTotalAmount().compareTo(stripeTotal) != 0) {
                // Tiền không khớp -> Báo động gian lận (Có thể gửi cảnh báo cho Admin ở đây)
                throw new RuntimeException("Số tiền thanh toán trên Stripe không khớp với đơn hàng gốc!");
            }

            // 3. Thanh toán Ok -> Tiến hành trừ tồn kho DỰA TRÊN ORDER ITEM (Bỏ qua Cart)
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                if (variant.getStockQuantity() < item.getQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + item.getProductName() + "' vừa hết hàng trong lúc bạn thanh toán. Vui lòng liên hệ CSKH để hoàn tiền!");
                }
                variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                variantRepository.save(variant);
            }

            // 4. Đổi trạng thái Order thành COMPLETED
            order.setStatus(OrderStatus.COMPLETED);
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .status(OrderStatus.COMPLETED)
                    .note("Thanh toán Stripe thành công. Đã trừ tồn kho.")
                    .build();
            order.addStatusHistory(history);

            // 5. Xóa giỏ hàng của user
            cartService.clearCart(order.getUser().getId());

            // 6. Bắn Event Kafka
            kafkaTemplate.send("order.created", new OrderCreatedEvent(
                    order.getUser().getEmail(), order.getTotalAmount().longValue(), "COMPLETED"));

            return orderRepository.save(order);

        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Lỗi xung đột dữ liệu", e);
            throw new RuntimeException("Rất tiếc, một trong những sản phẩm bạn chọn vừa mới hết hàng!");
        } catch (Exception e) {
            log.error("Lỗi khi xử lý đơn hàng Stripe", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    // --- 1. Xử lý tạo Order từ Kafka ---
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
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders(int limit) {
        // Tối ưu hiệu năng: Dùng PageRequest đẩy thẳng giới hạn (Limit) xuống DB thay vì lấy ra hết rồi stream().limit()
        return orderRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public List<Order> getAllOrdersForAdmin() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Order getOrderDetails(Long id, Long userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));
    }

    // --- 3. Biểu đồ thống kê ---
    public List<OrderChartResponse> getOrderChart() {
        // Gọi hàm mới tạo bên Repository
        List<Object[]> rawStats = orderRepository.getRawMonthlyStats(LocalDateTime.now().minusMonths(6));
        List<OrderChartResponse> responseList = new ArrayList<>();

        for (Object[] row : rawStats) {
            String month = (String) row[0];

            // Ép kiểu qua Number trước để an toàn với mọi loại số của MySQL (Long, BigInteger...)
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

        // 🌟 Lưu vết lịch sử người đổi trạng thái
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
    public Order createPendingOrder(Long userId, String stripeSessionId) {
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
                .status(OrderStatus.PENDING) // QUAN TRỌNG: Chỉ là chờ thanh toán
                .stripeSessionId(stripeSessionId) // Lưu lại mã phiên Stripe để đối chiếu sau
                .build();

        // Copy từ CartItem sang OrderItem (Khóa chết giá trị)
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

            // Chỉ check xem kho còn không, CHƯA TRỪ KHO vội
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + "' không đủ số lượng!");
            }

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productId(variant.getProduct().getId())
                    .productName(variant.getProduct().getName())
                    .sku(variant.getSku())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(variant.getPrice()) // Khóa cứng giá lúc mua
                    .build();
            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        // (Tùy chọn: Nếu bạn có logic tính toán Coupon thì gọi ở đây để trừ vào totalAmount)

        return orderRepository.save(order);
    }
}