package com.jooyeon.app.service.order;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.OrderException;
import com.jooyeon.app.domain.dto.order.OrderCreateRequestDto;
import com.jooyeon.app.domain.dto.order.OrderResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderItem;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import com.jooyeon.app.domain.entity.payment.Payment;
import com.jooyeon.app.domain.entity.product.Product;
import com.jooyeon.app.repository.OrderRepository;
import com.jooyeon.app.service.member.MemberService;
import com.jooyeon.app.service.payment.PaymentService;
import com.jooyeon.app.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final ProductService productService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderCreateRequestDto request) {
        try {
            // 1. 회원 검증
            Member member = validateMember(memberId);

            // 2. 상품 검증 및 재고 확인
            List<Product> products = validateAndReserveProducts(request.getItems());

            // 3. 주문 생성
            Order order = createOrderEntity(member, request, products);

            // 4. 결제 처리
            Payment payment = processPaymentForOrder(order, request.getIdempotencyKey());

            // 5. 주문 완료 처리
            completeOrder(order, payment);

            log.info("[ORDER] 주문 생성 성공: orderId={}, paymentId={}, totalAmount={}",
                       order.getId(), payment.getId(), order.getTotalAmount());

            return OrderResponseDto.convertToResponseDto(order);

        } catch (Exception e) {
            log.error("[ORDER] 멤버의 주문 생성 실패: {}, 멱등성 키: {}",
                        memberId, request.getIdempotencyKey(), e);

            rollbackStockReservation(request.getItems());
            throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_CREATION_FAILED, e);
        }
    }

    private Member validateMember(Long memberId) {
        return memberService.findMemberEntityById(memberId);
    }

    private List<Product> validateAndReserveProducts(List<OrderCreateRequestDto.OrderItemDto> items) {
        List<Long> productIds = items.stream()
            .map(OrderCreateRequestDto.OrderItemDto::getProductId)
            .collect(Collectors.toList());

        List<Product> products = productService.getProductsByIds(productIds);

        // 재고 확인
        for (OrderCreateRequestDto.OrderItemDto itemDto : items) {
            productService.checkStockAvailability(itemDto.getProductId(), itemDto.getQuantity());
        }

        // 재고 예약
        for (OrderCreateRequestDto.OrderItemDto itemDto : items) {
            productService.reserveStock(itemDto.getProductId(), itemDto.getQuantity());
        }

        return products;
    }

    private Order createOrderEntity(Member member, OrderCreateRequestDto request, List<Product> products) {
        Order order = new Order();
        order.setMember(member);
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey(request.getIdempotencyKey());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = createOrderItems(order, request.getItems(), products);
        BigDecimal totalAmount = calculateTotalAmount(orderItems);

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }

    private List<OrderItem> createOrderItems(Order order, List<OrderCreateRequestDto.OrderItemDto> itemDtos, List<Product> products) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateRequestDto.OrderItemDto itemDto : itemDtos) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(itemDto.getProductId()))
                .findFirst()
                .orElseThrow(() -> new OrderException(ErrorCode.PRODUCT_NOT_FOUND));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemDto.getQuantity()));
            orderItem.setTotalPrice(itemTotal);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment processPaymentForOrder(Order order, String idempotencyKey) {
        return paymentService.processPayment(order.getId(), idempotencyKey, "DEFAULT");
    }

    private void completeOrder(Order order, Payment payment) {
        order.setPaymentId(payment.getId());
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void rollbackStockReservation(List<OrderCreateRequestDto.OrderItemDto> items) {
        try {
            for (OrderCreateRequestDto.OrderItemDto itemDto : items) {
                productService.releaseStock(itemDto.getProductId(), itemDto.getQuantity());
            }
        } catch (Exception releaseException) {
            log.error("[ORDER] 롤백 중 재고 해제 실패", releaseException);
        }
    }

    public Page<OrderResponseDto> getOrdersByMember(Long memberId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return orders.map(OrderResponseDto::convertToResponseDto);
    }

    public OrderResponseDto getOrderById(Long orderId, Long memberId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_NOT_FOUND));

        return OrderResponseDto.convertToResponseDto(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);
        }

        try {
            if (order.getPaymentId() != null) {
                paymentService.cancelPayment(order.getPaymentId());
            }

            for (OrderItem item : order.getItems()) {
                productService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("[ORDER] 주문 취소 실패: {}", orderId, e);
            throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_CANCELLATION_FAILED, e);
        }
    }



}