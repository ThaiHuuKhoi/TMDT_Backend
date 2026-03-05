package com.KhoiCG.TMDT.modules.payment.repository;

import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
}