package com.jooyeon.app.service.payment;

import com.jooyeon.app.common.idempotency.IdempotencyService;
import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import com.jooyeon.app.domain.entity.payment.Payment;
import com.jooyeon.app.domain.entity.payment.PaymentStatus;
import com.jooyeon.app.repository.OrderRepository;
import com.jooyeon.app.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 멱등성 테스트")
class PaymentServiceIdempotencyTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private Payment testPayment;
    private IdempotencyService.IdempotencyResult duplicateResult;
    private IdempotencyService.IdempotencyResult newRequestResult;

    @BeforeEach
    void setUp() {
        // 테스트 주문 생성
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setCreatedAt(LocalDateTime.now());

        // 테스트 결제 생성
        testPayment = new Payment();
        testPayment.setId(100L);
        testPayment.setOrderId(1L);
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setPaymentStatus(PaymentStatus.SUCCESS);
        testPayment.setPaymentMethod("DEFAULT");
        testPayment.setTransactionId("TXN_TEST123");
        testPayment.setCreatedAt(LocalDateTime.now());

        duplicateResult = new IdempotencyService.IdempotencyResult(true, testPayment, null);
        newRequestResult = new IdempotencyService.IdempotencyResult(false, null, null);
    }

    @Test
    @DisplayName("멱등성 키 중복 - 기존 결제 결과 반환")
    void processPayment_DuplicateIdempotencyKey_ReturnsExistingPayment() {
        // given
        String idempotencyKey = "payment-123";
        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(duplicateResult);

        // when
        Payment result = paymentService.processPayment(1L, idempotencyKey, "DEFAULT");

        // then
        assertThat(result).isEqualTo(testPayment);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // 실제 결제 처리 로직이 실행되지 않았는지 확인
        verify(orderRepository, never()).findById(anyLong());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(idempotencyService, never()).saveResult(anyString(), any());
    }

    @Test
    @DisplayName("멱등성 키 처리 중 상태 - 예외 발생")
    void processPayment_IdempotencyKeyInProgress_ThrowsException() {
        // given
        String idempotencyKey = "payment-processing";
        IdempotencyService.IdempotencyResult processingResult =
            new IdempotencyService.IdempotencyResult(true, null, null);
        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(processingResult);

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(1L, idempotencyKey, "DEFAULT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment already in progress");

        verify(idempotencyService, never()).saveResult(anyString(), any());
        verify(idempotencyService, never()).markFailed(anyString());
    }

    @Test
    @DisplayName("새로운 멱등성 키 - 결제 처리 성공")
    void processPayment_NewIdempotencyKey_ProcessesPaymentSuccessfully() {
        // given
        String idempotencyKey = "payment-new-123";
        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(newRequestResult);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // when
        Payment result = paymentService.processPayment(1L, idempotencyKey, "DEFAULT");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(idempotencyService).checkIdempotency(idempotencyKey);
        verify(orderRepository).findById(1L);
        verify(paymentRepository).findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // PENDING -> SUCCESS
        verify(idempotencyService).saveResult(idempotencyKey, result);
    }

    @Test
    @DisplayName("이미 결제된 주문 - 기존 결제 반환 및 멱등성 키에 저장")
    void processPayment_AlreadyPaidOrder_ReturnsExistingPayment() {
        // given
        String idempotencyKey = "payment-existing-123";
        Payment existingPayment = new Payment();
        existingPayment.setId(200L);
        existingPayment.setOrderId(1L);
        existingPayment.setPaymentStatus(PaymentStatus.SUCCESS);

        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(newRequestResult);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Collections.singletonList(existingPayment));

        // when
        Payment result = paymentService.processPayment(1L, idempotencyKey, "DEFAULT");

        // then
        assertThat(result).isEqualTo(existingPayment);
        assertThat(result.getId()).isEqualTo(200L);

        verify(idempotencyService).saveResult(idempotencyKey, existingPayment);
        verify(paymentRepository, never()).save(any(Payment.class)); // 새로운 결제 생성하지 않음
    }

    @Test
    @DisplayName("결제 처리 실패 - 멱등성 키 실패 표시")
    void processPayment_ProcessingFails_MarksIdempotencyKeyAsFailed() {
        // given
        String idempotencyKey = "payment-fail-123";
        when(idempotencyService.checkIdempotency(idempotencyKey)).thenReturn(newRequestResult);
        when(orderRepository.findById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(1L, idempotencyKey, "DEFAULT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(idempotencyService).markFailed(idempotencyKey);
        verify(idempotencyService, never()).saveResult(anyString(), any());
    }


    @Test
    @DisplayName("동일한 멱등성 키로 동시 요청 - 첫 번째만 처리됨")
    void processPayment_ConcurrentSameIdempotencyKey_OnlyFirstProcessed() {
        // given
        String idempotencyKey = "payment-concurrent-123";

        // 첫 번째 호출: 새 요청
        // 두 번째 호출: 중복 요청 (이미 처리 완료)
        when(idempotencyService.checkIdempotency(idempotencyKey))
                .thenReturn(newRequestResult)
                .thenReturn(duplicateResult);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // when
        Payment result1 = paymentService.processPayment(1L, idempotencyKey, "DEFAULT");
        Payment result2 = paymentService.processPayment(1L, idempotencyKey, "DEFAULT");

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isEqualTo(testPayment); // 두 번째는 캐시된 결과
        assertThat(result1.getOrderId()).isEqualTo(result2.getOrderId());

        // 첫 번째 호출에서만 실제 처리 로직 실행
        verify(orderRepository, times(1)).findById(1L);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // 첫 번째 호출에서만
        verify(idempotencyService, times(1)).saveResult(eq(idempotencyKey), any());
    }

    @Test
    @DisplayName("다른 멱등성 키로 동일 주문 결제 - 각각 독립적 처리")
    void processPayment_DifferentIdempotencyKeys_ProcessedIndependently() {
        // given
        String idempotencyKey1 = "payment-key-1";
        String idempotencyKey2 = "payment-key-2";

        when(idempotencyService.checkIdempotency(anyString())).thenReturn(newRequestResult);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Collections.emptyList()); // 첫 번째 호출

        // 두 번째 호출에서는 이미 결제된 상태로 반환
        Payment firstPayment = new Payment();
        firstPayment.setId(300L);
        firstPayment.setPaymentStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.save(any(Payment.class))).thenReturn(firstPayment);

        // when
        Payment result1 = paymentService.processPayment(1L, idempotencyKey1, "DEFAULT");

        // 두 번째 요청에서는 이미 결제 완료된 상태
        when(paymentRepository.findByOrderIdAndPaymentStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Collections.singletonList(firstPayment));
        Payment result2 = paymentService.processPayment(1L, idempotencyKey2, "DEFAULT");

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getId()).isEqualTo(result2.getId()); // 같은 결제 결과

        verify(idempotencyService).checkIdempotency(idempotencyKey1);
        verify(idempotencyService).checkIdempotency(idempotencyKey2);
        verify(idempotencyService).saveResult(idempotencyKey1, result1);
        verify(idempotencyService).saveResult(idempotencyKey2, result2);
    }
}