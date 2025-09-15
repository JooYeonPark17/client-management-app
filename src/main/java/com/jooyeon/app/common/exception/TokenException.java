package com.jooyeon.app.common.exception;

import lombok.Getter;

@Getter
public class TokenException extends RuntimeException {

    private final ErrorCode errorCode;

    public TokenException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TokenException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

}
