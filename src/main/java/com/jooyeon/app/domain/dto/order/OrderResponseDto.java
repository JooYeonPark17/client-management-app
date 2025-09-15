package com.jooyeon.app.domain.dto.order;

import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class OrderResponseDto {

    private Long orderId;
    private Long memberId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Long paymentId;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDto {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
    }

    public static OrderResponseDto convertToResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
            .map(item -> new OrderItemResponseDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
            ))
            .collect(Collectors.toList());

        return new OrderResponseDto(
            order.getId(),
            order.getMember().getId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getPaymentId(),
            itemDtos,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}