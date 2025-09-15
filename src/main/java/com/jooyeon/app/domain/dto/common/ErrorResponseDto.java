package com.jooyeon.app.domain.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private final boolean success;
    private final String error;
    private final String errorCode;
    private final LocalDateTime timestamp;

    public static ErrorResponseDto of(String message, String errorCode) {
        return new ErrorResponseDto(false,message, errorCode, LocalDateTime.now());
    }
}