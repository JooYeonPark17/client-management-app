package com.jooyeon.app.service.payment;

import com.jooyeon.app.common.idempotency.IdempotencyService;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.payment.Payment;
import com.jooyeon.app.domain.entity.payment.PaymentStatus;
import com.jooyeon.app.repository.OrderRepository;
import com.jooyeon.app.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 결제 서비스 - IdempotencyService를 통한 멱등성 보장
 * 락 처리는 IdempotencyService에서 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;


    @Transactional
    public Payment processPayment(Long orderId, String idempotencyKey, String paymentMethod) {
        // 1. 멱등성 키 검증
        Payment existingResult = checkIdempotency(idempotencyKey);
        if (existingResult != null) {
            return existingResult;
        }

        try {
            // 2. 주문 조회 및 검증
            Order order = validateAndGetOrder(orderId);

            // 3. 기존 결제 확인
            Payment existingPayment = checkExistingPayment(orderId, idempotencyKey);
            if (existingPayment != null) {
                return existingPayment;
            }

            // 4. 결제 생성 및 처리
            Payment payment = createPayment(order, paymentMethod);
            executePayment(payment, order);

            // 5. 멱등성 키에 결과 저장
            idempotencyService.saveResult(idempotencyKey, payment);
            return payment;

        } catch (Exception e) {
            idempotencyService.markFailed(idempotencyKey);
            log.error("[PAYMENT] 결제 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Payment checkIdempotency(String idempotencyKey) {
        IdempotencyService.IdempotencyResult idempotencyResult = idempotencyService.checkIdempotency(idempotencyKey);

        if (idempotencyResult.isDuplicate()) {
            if (idempotencyResult.getExistingResult() != null) {
                return (Payment) idempotencyResult.getExistingResult();
            } else {
                throw new IllegalStateException("Payment already in progress");
            }
        }
        return null;
    }

    private Order validateAndGetOrder(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return orderOpt.get();
    }

    private Payment checkExistingPayment(Long orderId, String idempotencyKey) {
        List<Payment> existingPayments = paymentRepository.findByOrderIdAndPaymentStatus(
            orderId, PaymentStatus.SUCCESS);

        if (!existingPayments.isEmpty()) {
            log.warn("[PAYMENT] 이미 결제된 주문: orderId={}", orderId);
            Payment result = existingPayments.get(0);
            idempotencyService.saveResult(idempotencyKey, result);
            return result;
        }
        return null;
    }

    private Payment createPayment(Order order, String paymentMethod) {
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    private void executePayment(Payment payment, Order order) {
        boolean paymentSuccess = processExternalPayment(payment);

        if (paymentSuccess) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            updateOrderStatus(order);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing failed");
        }
    }

    /**
     * 주문 상태 업데이트 - 낙관적 락 적용
     * JPA @Version을 통한 동시성 제어
     */
    private void updateOrderStatus(Order order) {
        try {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order); // 낙관적 락 버전 체크
        } catch (Exception e) {
            log.error("[PAYMENT] 낙관적 락으로 인한 주문 상태 업데이트 실패: orderId={}",
                        order.getId(), e);
            throw new RuntimeException("Order update failed due to concurrent modification", e);
        }
    }

    /**
     * 외부 결제 게이트웨이 호출 시뮬레이션
     */
    private boolean processExternalPayment(Payment payment) {
        try {
            // 외부 결제 API 호출 시뮬레이션 (2초 지연)
            Thread.sleep(2000);

            // 90% 성공률로 시뮬레이션
            return Math.random() > 0.1;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 결제 취소 메서드
     */
    @Transactional
    public void cancelPayment(Long paymentId) {
        log.info("[PAYMENT] 결제 취소 요청: paymentId={}", paymentId);
        // TODO: 실제 결제 취소 로직 구현
    }

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}