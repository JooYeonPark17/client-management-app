package com.jooyeon.app.controller;

import com.jooyeon.app.common.security.CurrentUser;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.dto.order.OrderCreateRequestDto;
import com.jooyeon.app.domain.dto.order.OrderResponseDto;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Order creation and management APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "결제 처리와 함께 새로운 주문을 생성합니다")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request,
            @CurrentUser Member currentMember) {

        OrderResponseDto order = orderService.createOrder(currentMember.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("주문이 생성되었습니다.", order));
    }

    @GetMapping
    @Operation(summary = "내 주문 목록 조회", description = "로그인한 회원의 주문 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdersByMember(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser Member currentMember) {

        Page<OrderResponseDto> orders = orderService.getOrdersByMember(currentMember.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @Parameter(description = "주문 ID") @PathVariable Long orderId,
            @CurrentUser Member currentMember) {

        OrderResponseDto order = orderService.getOrderById(orderId, currentMember.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "주문 취소", description = "기존 주문을 취소합니다")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Parameter(description = "주문 ID") @PathVariable Long orderId,
            @CurrentUser Member currentMember) {

        orderService.cancelOrder(orderId, currentMember.getId());
        return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다.", null));
    }
}