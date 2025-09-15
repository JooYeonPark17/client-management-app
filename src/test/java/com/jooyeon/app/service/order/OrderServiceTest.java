package com.jooyeon.app.service.order;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.OrderException;
import com.jooyeon.app.domain.dto.order.OrderCreateRequestDto;
import com.jooyeon.app.domain.dto.order.OrderResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderItem;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import com.jooyeon.app.domain.entity.product.Product;
import com.jooyeon.app.domain.entity.product.ProductStatus;
import com.jooyeon.app.repository.OrderRepository;
import com.jooyeon.app.service.member.MemberService;
import com.jooyeon.app.service.payment.PaymentService;
import com.jooyeon.app.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private Member testMember;
    private Product testProduct1;
    private Product testProduct2;
    private OrderCreateRequestDto orderCreateRequest;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 테스트 회원
        testMember = new Member();
        testMember.setId(1L);
        testMember.setUserId("testUser");
        testMember.setMemberStatus(MemberStatus.ACTIVE);

        // 테스트 상품들
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setName("테스트 상품 1");
        testProduct1.setPrice(new BigDecimal("50.00"));
        testProduct1.setStatus(ProductStatus.AVAILABLE);

        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setName("테스트 상품 2");
        testProduct2.setPrice(new BigDecimal("30.00"));
        testProduct2.setStatus(ProductStatus.AVAILABLE);

        // 주문 생성 요청
        orderCreateRequest = new OrderCreateRequestDto();
        orderCreateRequest.setIdempotencyKey("test-order-123");

        OrderCreateRequestDto.OrderItemDto item1 = new OrderCreateRequestDto.OrderItemDto();
        item1.setProductId(1L);
        item1.setQuantity(2);

        OrderCreateRequestDto.OrderItemDto item2 = new OrderCreateRequestDto.OrderItemDto();
        item2.setProductId(2L);
        item2.setQuantity(1);

        orderCreateRequest.setItems(Arrays.asList(item1, item2));

        // 테스트 주문
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setMember(testMember);
        testOrder.setStatus(OrderStatus.PAID);
        testOrder.setTotalAmount(new BigDecimal("130.00"));
        testOrder.setIdempotencyKey("test-order-123");
        testOrder.setPaymentId(100L);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        // 주문 아이템들
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setProduct(testProduct1);
        orderItem1.setQuantity(2);
        orderItem1.setUnitPrice(testProduct1.getPrice());
        orderItem1.setTotalPrice(testProduct1.getPrice().multiply(new BigDecimal(2)));

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProduct(testProduct2);
        orderItem2.setQuantity(1);
        orderItem2.setUnitPrice(testProduct2.getPrice());
        orderItem2.setTotalPrice(testProduct2.getPrice());

        testOrder.setItems(Arrays.asList(orderItem1, orderItem2));
    }

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_Success() {
        // given
        when(orderRepository.findByIdempotencyKey("test-order-123")).thenReturn(Optional.empty());
        when(memberService.findMemberEntityById(1L)).thenReturn(testMember);
        when(productService.getProductsByIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(testProduct1, testProduct2));
        doNothing().when(productService).checkStockAvailability(anyLong(), anyInt());
        doNothing().when(productService).reserveStock(anyLong(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentService.processPayment(anyLong(), any(BigDecimal.class))).thenReturn(100L);

        // when
        OrderResponseDto result = orderService.createOrder(1L, orderCreateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("130.00"));
        // assertThat(result.getIdempotencyKey()).isEqualTo("test-order-123"); // OrderResponseDto에 없음

        verify(orderRepository).findByIdempotencyKey("test-order-123");
        verify(memberService).findMemberEntityById(1L);
        verify(productService).getProductsByIds(Arrays.asList(1L, 2L));
        verify(productService, times(2)).checkStockAvailability(anyLong(), anyInt());
        verify(productService, times(2)).reserveStock(anyLong(), anyInt());
        verify(paymentService).processPayment(anyLong(), any(BigDecimal.class));
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 - 멱등성 키 중복")
    void createOrder_DuplicateIdempotencyKey() {
        // given
        when(orderRepository.findByIdempotencyKey("test-order-123")).thenReturn(Optional.of(testOrder));

        // when
        OrderResponseDto result = orderService.createOrder(1L, orderCreateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        // assertThat(result.getIdempotencyKey()).isEqualTo("test-order-123"); // OrderResponseDto에 없음

        verify(orderRepository).findByIdempotencyKey("test-order-123");
        verify(memberService, never()).findMemberEntityById(anyLong());
        verify(productService, never()).getProductsByIds(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 - 상품을 찾을 수 없음")
    void createOrder_ProductNotFound() {
        // given
        when(orderRepository.findByIdempotencyKey("test-order-123")).thenReturn(Optional.empty());
        when(memberService.findMemberEntityById(1L)).thenReturn(testMember);
        when(productService.getProductsByIds(Arrays.asList(1L, 2L)))
                .thenReturn(Collections.singletonList(testProduct1)); // 상품 2가 없음

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(1L, orderCreateRequest))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CREATION_FAILED);

        verify(productService, times(2)).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("주문 생성 - 재고 부족")
    void createOrder_InsufficientStock() {
        // given
        when(orderRepository.findByIdempotencyKey("test-order-123")).thenReturn(Optional.empty());
        when(memberService.findMemberEntityById(1L)).thenReturn(testMember);
        when(productService.getProductsByIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(testProduct1, testProduct2));
        doThrow(new OrderException(ErrorCode.PRODUCT_OUT_OF_STOCK))
                .when(productService).checkStockAvailability(1L, 2);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(1L, orderCreateRequest))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CREATION_FAILED);

        verify(productService, times(2)).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("회원 주문 목록 조회 - 성공")
    void getOrdersByMember_Success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);

        when(orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .thenReturn(orderPage);

        // when
        Page<OrderResponseDto> result = orderService.getOrdersByMember(memberId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(orderRepository).findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    @Test
    @DisplayName("주문 상세 조회 - 성공")
    void getOrderById_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.of(testOrder));

        // when
        OrderResponseDto result = orderService.getOrderById(orderId, memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getMemberId()).isEqualTo(memberId);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
    }

    @Test
    @DisplayName("주문 상세 조회 - 주문을 찾을 수 없음")
    void getOrderById_OrderNotFound() {
        // given
        Long orderId = 999L;
        Long memberId = 1L;
        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getOrderById(orderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
    }

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        testOrder.setStatus(OrderStatus.PAID);

        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.of(testOrder));
        doNothing().when(paymentService).cancelPayment(100L);
        doNothing().when(productService).releaseStock(anyLong(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderService.cancelOrder(orderId, memberId);

        // then
        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
        verify(paymentService).cancelPayment(100L);
        verify(productService, times(2)).releaseStock(anyLong(), anyInt());
        verify(orderRepository).save(testOrder);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 - 주문을 찾을 수 없음")
    void cancelOrder_OrderNotFound() {
        // given
        Long orderId = 999L;
        Long memberId = 1L;
        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
        verify(paymentService, never()).cancelPayment(anyLong());
        verify(productService, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("주문 취소 - 이미 취소된 주문")
    void cancelOrder_AlreadyCancelled() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        testOrder.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.of(testOrder));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_CANCELLED);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
        verify(paymentService, never()).cancelPayment(anyLong());
        verify(productService, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("주문 취소 - 이미 취소된 주문")
    void cancelOrder_AlreadyCancelled_FromService() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        // 이미 취소된 상태로 변경하여 취소 불가 상태 시뮬레이션
        testOrder.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.of(testOrder));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_CANCELLED);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
        verify(paymentService, never()).cancelPayment(anyLong());
        verify(productService, never()).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("주문 취소 - 결제 취소 실패")
    void cancelOrder_PaymentCancellationFailed() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        testOrder.setStatus(OrderStatus.PAID);

        when(orderRepository.findByIdAndMemberId(orderId, memberId))
                .thenReturn(Optional.of(testOrder));
        doThrow(new RuntimeException("Payment cancellation failed"))
                .when(paymentService).cancelPayment(100L);

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, memberId))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANCELLATION_FAILED);

        verify(orderRepository).findByIdAndMemberId(orderId, memberId);
        verify(paymentService).cancelPayment(100L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}