package com.jooyeon.app.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member Errors
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "M002", "이미 존재하는 전화번호입니다."),
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "M003", "이미 탈퇴한 회원입니다."),
    MEMBER_INACTIVE(HttpStatus.BAD_REQUEST, "M004", "비활성화된 회원입니다."),
    MEMBER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "M005", "활성화되지 않은 회원입니다."),
    MEMBER_WITHDRAWAL_PENDING(HttpStatus.BAD_REQUEST, "M006", "탈퇴 대기 중인 회원입니다."),
    MEMBER_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M007", "회원 가입에 실패했습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M008", "잘못된 비밀번호입니다."),
    WITHDRAWAL_ALREADY_PENDING(HttpStatus.BAD_REQUEST, "M009", "이미 탈퇴 요청 중입니다."),
    WITHDRAWAL_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M010", "탈퇴 요청에 실패했습니다."),
    NO_PENDING_WITHDRAWAL(HttpStatus.BAD_REQUEST, "M011", "대기 중인 탈퇴 요청이 없습니다."),
    CANCELLATION_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "M012", "탈퇴 취소 기간이 만료되었습니다."),
    WITHDRAWAL_CANCELLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M013", "탈퇴 취소에 실패했습니다."),
    WITHDRAWAL_NOT_REQUESTED(HttpStatus.BAD_REQUEST, "M014", "탈퇴 요청이 없습니다."),

    // Order Errors
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "O002", "이미 존재하는 주문입니다."),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "O003", "취소할 수 없는 주문입니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "O004", "이미 취소된 주문입니다."),
    ORDER_CANCELLATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "O005", "현재 상태에서 주문을 취소할 수 없습니다."),
    ORDER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O006", "주문 생성에 실패했습니다."),
    ORDER_CANCELLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O007", "주문 취소에 실패했습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "O008", "주문 접근 권한이 없습니다."),
    INVALID_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "O009", "유효하지 않은 주문 항목입니다."),

    // Product Errors
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "P003", "판매 중단된 상품입니다."),

    // Payment Errors
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Y001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "Y002", "결제에 실패했습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "Y003", "이미 처리된 결제입니다."),
    PAYMENT_CANCELLED(HttpStatus.BAD_REQUEST, "Y004", "취소된 결제입니다."),

    // Authentication Errors
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "잘못된 인증 정보입니다."),
    TOKEN_VALIDATION_FAILED(HttpStatus.UNAUTHORIZED, "A005", "토큰 검증에 실패했습니다."),

    // Common Errors
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "C001", "입력 값 검증에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_KEY_ERROR(HttpStatus.BAD_REQUEST, "C004", "중복된 키 오류가 발생했습니다."),

    // Lock Errors
    LOCK_ACQUISITION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "L001", "락 획득에 실패했습니다."),
    LOCK_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "L002", "락 획득 시간이 초과되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}