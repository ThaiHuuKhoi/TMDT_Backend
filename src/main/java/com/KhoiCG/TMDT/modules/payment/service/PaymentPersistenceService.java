package com.KhoiCG.TMDT.modules.payment.service;

import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import com.KhoiCG.TMDT.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;

    /**
     * Lưu FAILED payment trong transaction độc lập để tránh bị rollback cùng transaction cha.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedPayment(Order order, Payment.PaymentMethod paymentMethod, String transactionId) {
        try {
            Payment failedPayment = Payment.builder()
                    .order(order)
                    .paymentMethod(paymentMethod)
                    .transactionId(transactionId)
                    .amount(order.getTotalAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();
            paymentRepository.save(failedPayment);
            log.warn("Đã lưu payment FAILED cho giao dịch: {}", transactionId);
        } catch (Exception e) {
            log.error("Không thể lưu FAILED payment cho giao dịch {}: {}", transactionId, e.getMessage());
        }
    }
}
