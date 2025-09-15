package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.payment.Payment;
import com.jooyeon.app.domain.entity.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);


    List<Payment> findByOrderIdAndPaymentStatus(Long orderId, PaymentStatus status);


}