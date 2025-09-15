package com.jooyeon.app.common.exception;

import lombok.Getter;

@Getter
public class OrderException extends RuntimeException {

    private final ErrorCode errorCode;

    public OrderException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OrderException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}