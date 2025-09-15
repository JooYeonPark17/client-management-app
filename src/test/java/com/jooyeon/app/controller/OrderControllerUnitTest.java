package com.jooyeon.app.controller;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.OrderException;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.order.OrderCreateRequestDto;
import com.jooyeon.app.domain.dto.order.OrderResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import com.jooyeon.app.service.order.OrderService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController 순수 단위 테스트")
class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Member testMember;
    private OrderCreateRequestDto validOrderRequest;
    private OrderResponseDto orderResponseDto;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(1L);
        testMember.setUserId("testUser123");
        testMember.setName("테스트 사용자");
        testMember.setMemberStatus(MemberStatus.ACTIVE);

        // 주문 생성 요청 DTO
        validOrderRequest = new OrderCreateRequestDto();
        validOrderRequest.setIdempotencyKey("test-order-key-123");

        OrderCreateRequestDto.OrderItemDto orderItem1 = new OrderCreateRequestDto.OrderItemDto();
        orderItem1.setProductId(1L);
        orderItem1.setQuantity(2);

        OrderCreateRequestDto.OrderItemDto orderItem2 = new OrderCreateRequestDto.OrderItemDto();
        orderItem2.setProductId(2L);
        orderItem2.setQuantity(1);

        validOrderRequest.setItems(Arrays.asList(orderItem1, orderItem2));

        // 주문 응답 DTO
        orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrderId(1L);
        orderResponseDto.setMemberId(1L);
        orderResponseDto.setStatus(OrderStatus.PAID);
        orderResponseDto.setTotalAmount(new BigDecimal("150.00"));
        orderResponseDto.setCreatedAt(LocalDateTime.now());
        orderResponseDto.setUpdatedAt(LocalDateTime.now());
        orderResponseDto.setItems(Collections.emptyList());
    }

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_Success() {
        // given
        when(orderService.createOrder(eq(1L), any(OrderCreateRequestDto.class)))
                .thenReturn(orderResponseDto);

        // when
        ResponseEntity<ApiResponse<OrderResponseDto>> response =
                orderController.createOrder(validOrderRequest, testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("주문이 생성되었습니다.");
        assertThat(response.getBody().getData().getOrderId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getMemberId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getBody().getData().getTotalAmount()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("주문 생성 - 상품이 존재하지 않는 경우")
    void createOrder_ProductNotFound() {
        // given
        when(orderService.createOrder(eq(1L), any(OrderCreateRequestDto.class)))
                .thenThrow(new OrderException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderController.createOrder(validOrderRequest, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 생성 - 재고 부족")
    void createOrder_InsufficientStock() {
        // given
        when(orderService.createOrder(eq(1L), any(OrderCreateRequestDto.class)))
                .thenThrow(new OrderException(ErrorCode.PRODUCT_OUT_OF_STOCK));

        // when & then
        assertThatThrownBy(() -> orderController.createOrder(validOrderRequest, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("내 주문 목록 조회 - 성공")
    void getOrdersByMember_Success() {
        // given
        List<OrderResponseDto> orders = Arrays.asList(orderResponseDto);
        Page<OrderResponseDto> orderPage = new PageImpl<>(orders, PageRequest.of(0, 20), 1);
        Pageable pageable = PageRequest.of(0, 20);

        when(orderService.getOrdersByMember(eq(1L), any(Pageable.class)))
                .thenReturn(orderPage);

        // when
        ResponseEntity<ApiResponse<Page<OrderResponseDto>>> response =
                orderController.getOrdersByMember(pageable, testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getOrderId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getData().getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 상세 조회 - 성공")
    void getOrderById_Success() {
        // given
        Long orderId = 1L;
        when(orderService.getOrderById(eq(orderId), eq(1L)))
                .thenReturn(orderResponseDto);

        // when
        ResponseEntity<ApiResponse<OrderResponseDto>> response =
                orderController.getOrderById(orderId, testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getOrderId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getMemberId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 상세 조회 - 주문이 존재하지 않는 경우")
    void getOrderById_OrderNotFound() {
        // given
        Long orderId = 999L;
        when(orderService.getOrderById(eq(orderId), eq(1L)))
                .thenThrow(new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderController.getOrderById(orderId, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 취소 - 성공")
    void cancelOrder_Success() {
        // given
        Long orderId = 1L;
        doNothing().when(orderService).cancelOrder(eq(orderId), eq(1L));

        // when
        ResponseEntity<ApiResponse<Void>> response =
                orderController.cancelOrder(orderId, testMember);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("주문이 취소되었습니다.");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("주문 취소 - 이미 취소된 주문")
    void cancelOrder_AlreadyCancelled() {
        // given
        Long orderId = 1L;
        doThrow(new OrderException(ErrorCode.ORDER_ALREADY_CANCELLED))
                .when(orderService).cancelOrder(eq(orderId), eq(1L));

        // when & then
        assertThatThrownBy(() -> orderController.cancelOrder(orderId, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 - 취소할 수 없는 상태")
    void cancelOrder_CancellationNotAllowed() {
        // given
        Long orderId = 1L;
        doThrow(new OrderException(ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED))
                .when(orderService).cancelOrder(eq(orderId), eq(1L));

        // when & then
        assertThatThrownBy(() -> orderController.cancelOrder(orderId, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("주문 취소 - 존재하지 않는 주문")
    void cancelOrder_OrderNotFound() {
        // given
        Long orderId = 999L;
        doThrow(new OrderException(ErrorCode.ORDER_NOT_FOUND))
                .when(orderService).cancelOrder(eq(orderId), eq(1L));

        // when & then
        assertThatThrownBy(() -> orderController.cancelOrder(orderId, testMember))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
}