package com.jooyeon.app.service.payment;

import com.jooyeon.app.common.idempotency.IdempotencyService;
import com.jooyeon.app.common.lock.Lock;
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
import java.util.concurrent.TimeUnit;

/**
 * 결제 서비스 - 분산락과 멱등성 키 적용
 * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;


    /**
     * 결제 처리 - 분산락과 멱등성 키로 중복 결제 방지
     * 현재는 로컬 구현이지만, 분산 환경에서는 Redis 분산락이 필요합니다
     *
     * Redis 분산락 키 패턴: "payment:order:{orderId}"
     * 멱등성 키: 클라이언트가 제공하는 고유 키
     */
    @Lock(key = "'payment:order:' + #orderId", waitTime = 10, leaseTime = 30, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public Payment processPayment(Long orderId, String idempotencyKey, String paymentMethod) {

        // 1. 멱등성 키 검증 - 중복 결제 방지
        IdempotencyService.IdempotencyResult idempotencyResult = idempotencyService.checkIdempotency(idempotencyKey);

        if (idempotencyResult.isDuplicate()) {
            if (idempotencyResult.getExistingResult() != null) {
                return (Payment) idempotencyResult.getExistingResult();
            } else {
                throw new IllegalStateException("Payment already in progress");
            }
        }

        try {
            // 2. 주문 조회 및 검증 (낙관적 락 적용)
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                throw new IllegalArgumentException("Order not found: " + orderId);
            }
            Order order = orderOpt.get();

            // 3. 기존 결제 내역 확인
            List<Payment> existingPayments = paymentRepository.findByOrderIdAndPaymentStatus(
                orderId, PaymentStatus.SUCCESS);

            if (!existingPayments.isEmpty()) {
                log.warn("[PAYMENT] 이미 결제된 주문: orderId={}", orderId);
                Payment result = existingPayments.get(0);
                idempotencyService.saveResult(idempotencyKey, result);
                return result;
            }

            // 4. 결제 처리
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setTransactionId(generateTransactionId());
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            payment = paymentRepository.save(payment);

            // 5. 외부 결제 게이트웨이 호출 시뮬레이션
            boolean paymentSuccess = processExternalPayment(payment);

            if (paymentSuccess) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                paymentRepository.save(payment);

                // 6. 주문 상태 업데이트 (낙관적 락 버전 체크)
                updateOrderStatus(order);
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new RuntimeException("Payment processing failed");
            }

            // 7. 멱등성 키에 결과 저장
            idempotencyService.saveResult(idempotencyKey, payment);
            return payment;

        } catch (Exception e) {
            // 8. 실패시 멱등성 키 초기화
            idempotencyService.markFailed(idempotencyKey);
            log.error("[PAYMENT] 결제 처리 실패: {}", e.getMessage(), e);
            throw e;
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
     * 간편 결제 처리 메서드 - OrderService에서 사용
     */
    @Transactional
    public Long processPayment(Long orderId, java.math.BigDecimal amount) {
        String idempotencyKey = "payment_" + orderId + "_" + java.time.Instant.now().toEpochMilli();
        String paymentMethod = "DEFAULT";
        Payment payment = processPayment(orderId, idempotencyKey, paymentMethod);
        return payment.getId();
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