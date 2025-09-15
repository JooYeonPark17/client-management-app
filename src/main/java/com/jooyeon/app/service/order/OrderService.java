package com.jooyeon.app.service.order;

import com.jooyeon.app.common.exception.ErrorCode;
import com.jooyeon.app.common.exception.OrderException;
import com.jooyeon.app.domain.dto.order.OrderCreateRequestDto;
import com.jooyeon.app.domain.dto.order.OrderResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderItem;
import com.jooyeon.app.domain.entity.order.OrderStatus;
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
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentHashMap<String, Object> idempotencyCache = new ConcurrentHashMap<>();

    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderCreateRequestDto request) {
        log.info("[ORDER] 멤버를 위한 주문 생성: {} 멱등성 키: {}",
                   memberId, request.getIdempotencyKey());

        synchronized (getIdempotencyLock(request.getIdempotencyKey())) {
            Order existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
            if (existingOrder != null) {
                log.info("[ORDER] 멱등성 키에 대한 주문이 이미 존재: {}", request.getIdempotencyKey());
                return OrderResponseDto.convertToResponseDto(existingOrder);
            }

            try {
                Member member = memberService.findMemberEntityById(memberId);

                List<Long> productIds = request.getItems().stream()
                    .map(OrderCreateRequestDto.OrderItemDto::getProductId)
                    .collect(Collectors.toList());

                List<Product> products = productService.getProductsByIds(productIds);

                for (OrderCreateRequestDto.OrderItemDto itemDto : request.getItems()) {
                    productService.checkStockAvailability(itemDto.getProductId(), itemDto.getQuantity());
                }

                Order order = new Order();
                order.setMember(member);
                order.setStatus(OrderStatus.PENDING);
                order.setIdempotencyKey(request.getIdempotencyKey());
                order.setCreatedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());

                BigDecimal totalAmount = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();

                for (OrderCreateRequestDto.OrderItemDto itemDto : request.getItems()) {
                    Product product = products.stream()
                        .filter(p -> p.getId().equals(itemDto.getProductId()))
                        .findFirst()
                        .orElseThrow(() -> new OrderException(ErrorCode.PRODUCT_NOT_FOUND));

                    productService.reserveStock(product.getId(), itemDto.getQuantity());

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(product);
                    orderItem.setQuantity(itemDto.getQuantity());
                    orderItem.setUnitPrice(product.getPrice());

                    BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemDto.getQuantity()));
                    orderItem.setTotalPrice(itemTotal);
                    orderItems.add(orderItem);

                    totalAmount = totalAmount.add(itemTotal);
                }

                order.setItems(orderItems);
                order.setTotalAmount(totalAmount);
                order = orderRepository.save(order);

                Long paymentId = paymentService.processPayment(order.getId(), totalAmount);
                order.setPaymentId(paymentId);
                order.setStatus(OrderStatus.PAID);
                order.setUpdatedAt(LocalDateTime.now());
                order = orderRepository.save(order);

                log.info("[ORDER] 주문 생성 성공: orderId={}, paymentId={}, totalAmount={}",
                           order.getId(), paymentId, totalAmount);

                return OrderResponseDto.convertToResponseDto(order);

            } catch (Exception e) {
                log.error("[ORDER] 멤버의 주문 생성 실패: {}, 멱등성 키: {}",
                            memberId, request.getIdempotencyKey(), e);

                try {
                    for (OrderCreateRequestDto.OrderItemDto itemDto : request.getItems()) {
                        productService.releaseStock(itemDto.getProductId(), itemDto.getQuantity());
                    }
                } catch (Exception releaseException) {
                    log.error("[ORDER] 롤백 중 재고 해제 실패", releaseException);
                }

                throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_CREATION_FAILED, e);
            }
        }
    }

    public Page<OrderResponseDto> getOrdersByMember(Long memberId, Pageable pageable) {
        log.debug("[ORDER] 멤버의 주문 목록 조회: {} - page: {}, size: {}",
                    memberId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return orders.map(OrderResponseDto::convertToResponseDto);
    }

    public OrderResponseDto getOrderById(Long orderId, Long memberId) {
        log.debug("[ORDER] 멤버의 주문 조회: {} 멤버: {}", orderId, memberId);

        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_NOT_FOUND));

        return OrderResponseDto.convertToResponseDto(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        log.info("[ORDER] 멤버의 주문 취소: {} 멤버: {}", orderId, memberId);

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

            log.info("[ORDER] 주문 취소 성공: {}", orderId);

        } catch (Exception e) {
            log.error("[ORDER] 주문 취소 실패: {}", orderId, e);
            throw new OrderException(com.jooyeon.app.common.exception.ErrorCode.ORDER_CANCELLATION_FAILED, e);
        }
    }


    private Object getIdempotencyLock(String idempotencyKey) {
        return idempotencyCache.computeIfAbsent(idempotencyKey, k -> new Object());
    }

}