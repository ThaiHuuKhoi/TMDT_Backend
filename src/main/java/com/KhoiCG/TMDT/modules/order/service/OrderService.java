package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import com.KhoiCG.TMDT.modules.coupon.repository.CouponRepository;
import com.KhoiCG.TMDT.modules.coupon.service.CouponService;
import com.KhoiCG.TMDT.modules.order.dto.*;
import com.KhoiCG.TMDT.modules.order.entity.*;
import com.KhoiCG.TMDT.modules.order.event.OrderCompletedEvent;
import com.KhoiCG.TMDT.modules.order.mapper.OrderMapper;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.shipping.service.ShippingQuoteService;
import com.KhoiCG.TMDT.modules.payment.dto.ShippingDetailsRequest;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.repository.PaymentRepository;
import com.KhoiCG.TMDT.modules.payment.service.PaymentPersistenceService;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import com.KhoiCG.TMDT.modules.product.service.InventoryService;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private static final List<OrderStatus> REVENUE_AND_TOP_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED
    );

    private final OrderRepository orderRepository;
    private final UserRepo userRepo;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceService paymentPersistenceService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final ShippingQuoteService shippingQuoteService;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public void createOrder(PaymentSuccessEvent event) {
        try {
            User user = userRepo.findById(Long.valueOf(event.getUserId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

            OrderStatus status;
            try {
                status = OrderStatus.valueOf(event.getStatus().toUpperCase());
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", "Invalid order status from event");
            }

            Order order = Order.builder()
                    .user(user)
                    .totalAmount(BigDecimal.valueOf(event.getAmount()))
                    .status(status)
                    .statusHistories(new ArrayList<>())
                    .build();

            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .status(order.getStatus())
                    .note("Tạo đơn hàng từ hệ thống nội bộ")
                    .build();
            order.getStatusHistories().add(history);

            Order savedOrder = orderRepository.save(order);

            log.info("Order created for user: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Error creating order", e);
            throw e;
        }
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }
    public List<Order> getAllOrders(int limit) {
        return orderRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    public OrderPageResponse getAllOrdersPageForAdmin(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<Order> orderPage = orderRepository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return OrderPageResponse.builder()
                .items(orderPage.getContent().stream().map(orderMapper::toOrderResponse).toList())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalItems(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }

    /**
     * @param limit nếu {@code null} hoặc {@code <= 0} thì trả về toàn bộ đơn (mới nhất trước).
     */
    public List<OrderResponse> getAllOrdersForAdmin(Integer limit) {
        if (limit != null && limit > 0) {
            return orderRepository
                    .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent()
                    .stream()
                    .map(orderMapper::toOrderResponse)
                    .toList();
        }
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    public OrderResponse getOrderDetails(Long id, Long userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));
        return orderMapper.toOrderResponse(order);
    }

    public OrderResponse getOrderForAdmin(Long id) {
        Order order = orderRepository.findDetailForAdmin(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng."));
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrderByAdmin(AdminOrderCreateRequest req) {
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .stripeSessionId("ADMIN-" + UUID.randomUUID())
                .shippingName(req.getShippingName() != null && !req.getShippingName().isBlank()
                        ? req.getShippingName().trim() : user.getName())
                .shippingPhone(req.getShippingPhone() != null && !req.getShippingPhone().isBlank()
                        ? req.getShippingPhone().trim() : "0000000000")
                .shippingAddress(req.getShippingAddress() != null && !req.getShippingAddress().isBlank()
                        ? req.getShippingAddress().trim() : "—")
                .statusHistories(new ArrayList<>())
                .build();

        order.addStatusHistory(OrderStatusHistory.builder()
                .status(OrderStatus.PENDING)
                .note("Tạo đơn thủ công bởi quản trị viên")
                .build());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (AdminOrderLineRequest line : req.getLines()) {
            ProductVariant variant = productVariantRepository.findById(line.getVariantId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                            "Không tìm thấy biến thể sản phẩm: " + line.getVariantId()));

            if (variant.getStockQuantity() < line.getQuantity()) {
                throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                        "Sản phẩm '" + variant.getProduct().getName() + "' không đủ số lượng!");
            }

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productId(variant.getProduct().getId())
                    .productName(variant.getProduct().getName())
                    .sku(variant.getSku())
                    .quantity(line.getQuantity())
                    .priceAtPurchase(variant.getPrice())
                    .build();
            order.addOrderItem(orderItem);
        }

        String couponCode = req.getCouponCode();
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
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUPON",
                        "Mã giảm giá không hợp lệ: " + couponCheck.getMessage());
            }
        }

        int itemUnits = req.getLines().stream().mapToInt(AdminOrderLineRequest::getQuantity).sum();
        long shippingFeeVnd = shippingQuoteService.computeFeeVndForItemCount(itemUnits);
        order.setShippingFee(BigDecimal.valueOf(shippingFeeVnd));
        totalAmount = totalAmount.add(BigDecimal.valueOf(shippingFeeVnd));

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        return orderMapper.toOrderResponse(saved);
    }

    @Transactional
    public void bulkCancelOrdersForAdmin(List<Long> ids) {
        for (Long id : ids) {
            try {
                updateOrderStatus(id, OrderStatus.CANCELLED.name());
            } catch (ApiException ex) {
                log.warn("Bỏ qua hủy đơn {}: {}", id, ex.getMessage());
            }
        }
    }

    public AdminDashboardStatsResponse getAdminDashboardStats() {
        BigDecimal revenue = orderRepository.sumTotalAmountByStatusIn(REVENUE_AND_TOP_STATUSES);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }
        long totalOrders = orderRepository.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countOrdersGroupedByStatus()) {
            OrderStatus st = (OrderStatus) row[0];
            byStatus.put(st.name(), ((Number) row[1]).longValue());
        }

        List<OrderChartResponse> monthlyOrders = getOrderChart();

        List<Object[]> revRows = orderRepository.getMonthlyRevenueStats(
                LocalDateTime.now().minusMonths(6), REVENUE_AND_TOP_STATUSES);
        List<MonthlyRevenuePoint> revenuePoints = new ArrayList<>();
        for (Object[] r : revRows) {
            String month = (String) r[0];
            BigDecimal amt = r[1] instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) r[1]).doubleValue());
            revenuePoints.add(new MonthlyRevenuePoint(month, amt));
        }

        List<Object[]> topRaw = orderRepository.findTopSellingProducts(
                REVENUE_AND_TOP_STATUSES, PageRequest.of(0, 5));
        List<TopProductStatResponse> topProducts = new ArrayList<>();
        for (Object[] r : topRaw) {
            Long pid = ((Number) r[0]).longValue();
            String name = (String) r[1];
            long units = ((Number) r[2]).longValue();
            BigDecimal lineRev = r[3] instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) r[3]).doubleValue());
            topProducts.add(TopProductStatResponse.builder()
                    .productId(pid)
                    .productName(name)
                    .unitsSold(units)
                    .revenue(lineRev)
                    .build());
        }

        return AdminDashboardStatsResponse.builder()
                .totalRevenue(revenue)
                .totalOrders(totalOrders)
                .ordersByStatus(byStatus)
                .monthlyOrders(monthlyOrders)
                .monthlyRevenue(revenuePoints)
                .topProducts(topProducts)
                .build();
    }

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

    @Transactional
    public Order updateOrderStatus(Long id, String newStatusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));

        OrderStatus previous = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", "Trạng thái đơn hàng không hợp lệ");
        }
        if (!canTransition(previous, newStatus)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ORDER_TRANSITION",
                    "Không thể chuyển trạng thái từ " + previous + " sang " + newStatus
            );
        }

        if (previous == OrderStatus.PENDING && newStatus == OrderStatus.PAID) {
            // Chỉ trừ kho nếu chưa reserve (admin tạo tay). VNPay orders đã trừ lúc pending.
            if (!order.isInventoryReserved()) {
                try {
                    inventoryService.deductInventoryForOrder(order.getItems());
                } catch (Exception e) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_DEDUCT_FAILED",
                            "Không thể trừ tồn kho: " + e.getMessage());
                }
            }
            if (order.getCoupon() != null) {
                Coupon coupon = order.getCoupon();
                coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
                couponRepository.save(coupon);
            }
        }

        if (previous == OrderStatus.PENDING && newStatus == OrderStatus.CANCELLED
                && order.isInventoryReserved()) {
            inventoryService.releaseInventoryForOrder(order.getItems());
            order.setInventoryReserved(false);
        }

        order.setStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .note("Quản trị viên cập nhật trạng thái")
                .build();
        order.getStatusHistories().add(history);

        return orderRepository.save(order);
    }

    @Transactional
    public Order createPendingOrder(Long userId, String paymentTxnRef, String couponCode, ShippingDetailsRequest shipping) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));

        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_EMPTY", "Giỏ hàng trống, không thể tạo đơn hàng!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Khởi tạo Order trạng thái PENDING (paymentTxnRef: mã tham chiếu VNPay lưu tại stripe_session_id)
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .stripeSessionId(paymentTxnRef)
                .shippingName(shipping.getName())
                .shippingPhone(shipping.getPhone())
                .shippingAddress(shipping.toFormattedAddress())
                .build();
        order.addStatusHistory(OrderStatusHistory.builder()
                .status(OrderStatus.PENDING)
                .note("Đơn hàng tạo mới, chờ thanh toán VNPay")
                .build());

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

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

        // Trừ kho ngay lúc tạo pending để tránh oversell trong thời gian user thanh toán VNPay.
        inventoryService.deductInventoryForOrder(order.getItems());
        order.setInventoryReserved(true);

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
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUPON",
                        "Mã giảm giá không hợp lệ: " + couponCheck.getMessage());
            }
        }

        int itemUnits = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        long shippingFeeVnd = shippingQuoteService.computeFeeVndForItemCount(itemUnits);
        order.setShippingFee(BigDecimal.valueOf(shippingFeeVnd));
        totalAmount = totalAmount.add(BigDecimal.valueOf(shippingFeeVnd));

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrderPayment(String sessionId, Payment.PaymentMethod paymentMethod) {

        if (paymentRepository.findByTransactionId(sessionId).isPresent()) {
            log.info("Giao dịch {} đã được xử lý trước đó. Bỏ qua để chống trùng lặp.", sessionId);
            return orderRepository.findByStripeSessionId(sessionId).orElse(null);
        }

        Order order = orderRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy giao dịch này!"));

        if (order.getStatus() != OrderStatus.PENDING) {
            return order;
        }

        try {
            // Kho đã được trừ lúc tạo pending (VNPay). Admin orders chưa trừ thì trừ ở đây.
            if (!order.isInventoryReserved()) {
                inventoryService.deductInventoryForOrder(order.getItems());
            }

            if (order.getCoupon() != null) {
                Coupon coupon = order.getCoupon();
                coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
                couponRepository.save(coupon);
            }

            order.setStatus(OrderStatus.PAID);
            order.addStatusHistory(OrderStatusHistory.builder()
                    .status(OrderStatus.PAID)
                    .note("Thanh toán hoàn tất (" + paymentMethod + ").")
                    .build());

            Order savedOrder = orderRepository.save(order);

            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .paymentMethod(paymentMethod)
                    .transactionId(sessionId)
                    .amount(savedOrder.getTotalAmount())
                    .status(Payment.PaymentStatus.SUCCESS)
                    .build();
            paymentRepository.save(payment);

            eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder));

            return savedOrder;

        } catch (Exception e) {
            log.error("Lỗi khi hoàn tất đơn hàng: ", e);
            if (paymentRepository.findByTransactionId(sessionId).isEmpty()) {
                paymentPersistenceService.saveFailedPayment(order, paymentMethod, sessionId);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_CONFIRM_FAILED",
                    "Xử lý thanh toán thất bại: " + e.getMessage());
        }
    }

    private boolean canTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        return ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus);
    }
}