package com.jooyeon.app.common.exception;

import com.jooyeon.app.domain.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(MemberException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 회원 관련 오류: {} - URI: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderException(OrderException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 주문 관련 오류: {} - URI: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductException(ProductException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 상품 관련 오류: {} - URI: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 결제 관련 오류: {} - URI: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenException(TokenException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 토큰 관련 오류: {} - URI: {}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("[EXCEPTION_HANDLER] 입력 값 검증 실패: {} - URI: {}", e.getMessage(), request.getRequestURI());

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing // Keep first error message if duplicate fields
                ));

        ApiResponse<Void> response = ApiResponse.validationError("입력 값 검증에 실패했습니다.", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("[EXCEPTION_HANDLER] 바인딩 오류: {}", e.getMessage());

        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
                ));

        ApiResponse<Void> response = ApiResponse.validationError("요청 데이터 바인딩에 실패했습니다.", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[EXCEPTION_HANDLER] 파라미터 타입 불일치: parameter={}, value={}, requiredType={}",
                   e.getName(), e.getValue(), e.getRequiredType());

        String errorMessage = String.format("파라미터 '%s'의 값 '%s'이(가) 잘못되었습니다. 예상 타입: %s",
                                           e.getName(), e.getValue(), e.getRequiredType().getSimpleName());
        ApiResponse<Void> response = ApiResponse.error(errorMessage, ErrorCode.VALIDATION_ERROR.getCode());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[EXCEPTION_HANDLER] 잘못된 인수: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(e.getMessage(), ErrorCode.VALIDATION_ERROR.getCode());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("[EXCEPTION_HANDLER] 예상치 못한 런타임 오류", e);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("[EXCEPTION_HANDLER] 예상치 못한 오류", e);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.internalServerError().body(response);
    }

}