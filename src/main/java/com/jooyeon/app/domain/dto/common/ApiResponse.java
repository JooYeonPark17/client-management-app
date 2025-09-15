package com.jooyeon.app.domain.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jooyeon.app.common.exception.ErrorCode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private Map<String, String> fieldErrors;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "요청이 성공적으로 처리되었습니다.";
        return response;
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "요청이 성공적으로 처리되었습니다.";
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = errorCode.getMessage();
        response.errorCode = errorCode.getCode();
        return response;
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.errorCode = errorCode;
        return response;
    }

    public static <T> ApiResponse<T> validationError(String message, Map<String, String> fieldErrors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.errorCode = ErrorCode.VALIDATION_ERROR.getCode();
        response.fieldErrors = fieldErrors;
        return response;
    }

}