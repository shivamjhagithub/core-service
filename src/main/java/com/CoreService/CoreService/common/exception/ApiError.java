package com.CoreService.CoreService.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Error payload. Deliberately free of stack traces and internal details.
 */
@Getter
@Builder
public class ApiError {

    private final boolean success;
    private final String message;
    private final ErrorCode errorCode;
    private final Instant timestamp;
    private final String path;
    private final Map<String, String> fieldErrors;

    public static ApiError of(String message, ErrorCode errorCode, String path) {
        return ApiError.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }
}
