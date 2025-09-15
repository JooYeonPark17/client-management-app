package com.jooyeon.app.controller;

import com.jooyeon.app.common.security.CurrentUser;
import com.jooyeon.app.domain.dto.common.ApiResponse;
import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "결제 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "결제 상태 확인", description = "결제 ID로 결제 상태를 확인합니다")
    public ResponseEntity<ApiResponse<String>> getPaymentStatus(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId,
            @CurrentUser Member currentMember) {

        // TODO: 실제 결제 상태 조회 로직 구현
        String status = "COMPLETED"; // 임시 응답

        return ResponseEntity.ok(ApiResponse.success("결제 상태 조회가 완료되었습니다.", status));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "주문 결제 내역 조회", description = "주문 ID로 결제 내역을 조회합니다")
    public ResponseEntity<ApiResponse<String>> getPaymentByOrderId(
            @Parameter(description = "주문 ID") @PathVariable Long orderId,
            @CurrentUser Member currentMember) {

        // TODO: 실제 결제 내역 조회 로직 구현
        String paymentInfo = "결제 정보"; // 임시 응답

        return ResponseEntity.ok(ApiResponse.success("결제 내역 조회가 완료되었습니다.", paymentInfo));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "결제 취소", description = "결제를 취소합니다")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId,
            @CurrentUser Member currentMember) {

        // TODO: 실제 결제 취소 로직 구현
        return ResponseEntity.ok(ApiResponse.success("결제가 취소되었습니다.", null));
    }
}