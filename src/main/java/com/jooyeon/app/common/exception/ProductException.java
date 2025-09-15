package com.jooyeon.app.common.exception;

import lombok.Getter;

@Getter
public class ProductException extends RuntimeException {

    private final ErrorCode errorCode;

    public ProductException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ProductException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}