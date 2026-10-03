package com.appointflow.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(HttpStatus status, String message) {
        this(status, message, status.name());
    }

    // Kısayollar
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, "NOT_FOUND");
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST");
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, "CONFLICT");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, "FORBIDDEN");
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, "UNAUTHORIZED");
    }

    public static ApiException quotaExceeded(String message) {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED, message, "QUOTA_EXCEEDED");
    }

    public static ApiException internalError(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, "INTERNAL_SERVER_ERROR");
    }
}
